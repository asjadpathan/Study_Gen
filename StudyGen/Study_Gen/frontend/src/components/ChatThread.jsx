import React, { useState, useEffect, useRef } from 'react';
import Button from './Button';
import { useToast } from '../context/ToastContext';
import axiosInstance from '../services/axiosInstance';
import styles from './ChatThread.module.css';

const SUGGESTION_CHIPS = [
  'What are the key operators in Python?',
  'Explain Dependency Injection vs Inversion of Control',
  'How do I optimize database query latency?',
  'What are common architectural pitfalls in microservices?'
];

const API_BASE = 'http://localhost:8080/api';

const ChatThread = ({
  sessionId,
  contextModule = null,
  initialModuleId = null,
  onSessionCreated = null,
  isDrawer = false,
  className = ''
}) => {
  const { addToast } = useToast();

  const [messages, setMessages] = useState([]);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [contextMeta, setContextMeta] = useState({
    roadmapCount: 0,
    moduleCount: 0,
    documentCount: 0,
    activeAssessmentModule: null
  });

  // Composer state
  const [input, setInput] = useState('');
  const [isStreaming, setIsStreaming] = useState(false);
  const [streamingError, setStreamingError] = useState(null);
  const [rateLimitBanner, setRateLimitBanner] = useState(false);

  // Active citation popover: { id, title, source, snippet, link }
  const [activePopover, setActivePopover] = useState(null);
  const [copiedMsgId, setCopiedMsgId] = useState(null);

  const messagesEndRef = useRef(null);
  const textareaRef = useRef(null);
  const abortControllerRef = useRef(null);

  // Helper to extract JWT token from either 'token' or 'studygen_user'
  const getToken = () => {
    const direct = localStorage.getItem('token');
    if (direct) return direct;
    try {
      const stored = localStorage.getItem('studygen_user');
      return stored ? JSON.parse(stored).token : null;
    } catch {
      return null;
    }
  };

  // Assessment in progress flag check (shared via localStorage or prop)
  const assessmentInProgress = Boolean(
    contextMeta.activeAssessmentModule ||
    localStorage.getItem('studygen_active_assessment')
  );

  // Auto-scroll on new messages or stream tokens
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isStreaming]);

  // Fetch context metadata
  useEffect(() => {
    const fetchMeta = async () => {
      try {
        const res = await axiosInstance.get('/chat/context-meta');
        if (res.data) setContextMeta(res.data);
      } catch (err) {
        console.warn('Could not load chat context meta:', err);
      }
    };
    fetchMeta();
  }, []);

  // Fetch session message history when sessionId changes
  useEffect(() => {
    if (!sessionId) {
      setMessages([]);
      return;
    }

    const fetchHistory = async () => {
      setLoadingHistory(true);
      setStreamingError(null);
      try {
        const res = await axiosInstance.get(`/chat/sessions/${sessionId}`);
        if (res.data?.messages) {
          setMessages(res.data.messages);
        }
      } catch (err) {
        console.error('Failed to load session history:', err);
      } finally {
        setLoadingHistory(false);
      }
    };

    fetchHistory();
  }, [sessionId]);

  // Auto-grow textarea height
  const handleTextareaChange = (e) => {
    const val = e.target.value;
    if (val.length <= 2000) {
      setInput(val);
      if (textareaRef.current) {
        textareaRef.current.style.height = 'auto';
        textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 180)}px`;
      }
    }
  };

  // SSE Streaming Fetch implementation
  const handleSendMessage = async (textToSend = null) => {
    const messageContent = (textToSend || input).trim();
    if (!messageContent || isStreaming) return;

    setInput('');
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
    setStreamingError(null);
    setRateLimitBanner(false);

    let activeSessionId = sessionId;

    // If no active session, create one first
    if (!activeSessionId) {
      try {
        const sessionTitle = messageContent.slice(0, 30) + (messageContent.length > 30 ? '...' : '');
        const targetModuleId = initialModuleId || contextModule?.id || null;
        const createRes = await axiosInstance.post('/chat/sessions', {
          title: sessionTitle,
          contextTag: targetModuleId ? 'MODULE' : 'GENERAL',
          moduleId: targetModuleId
        });
        activeSessionId = createRes.data?.id;
        if (onSessionCreated) onSessionCreated(createRes.data.id);
      } catch (err) {
        console.error('Failed to create session:', err);
        setStreamingError('Could not initialize a new chat session. Please try again.');
        return;
      }
    }

    // Add user message to UI
    const userMsgId = Date.now();
    const newUserMsg = {
      id: userMsgId,
      role: 'USER',
      content: messageContent,
      citations: [],
      createdAt: new Date().toISOString()
    };

    // Add placeholder assistant message
    const assistantMsgId = userMsgId + 1;
    const newAssistantMsg = {
      id: assistantMsgId,
      role: 'ASSISTANT',
      content: '',
      citations: [],
      createdAt: new Date().toISOString()
    };

    setMessages((prev) => [...prev, newUserMsg, newAssistantMsg]);
    setIsStreaming(true);

    const abortController = new AbortController();
    abortControllerRef.current = abortController;

    const token = getToken();

    try {
      const response = await fetch(`${API_BASE}/chat/sessions/${activeSessionId}/messages`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {})
        },
        body: JSON.stringify({
          message: messageContent,
          moduleId: initialModuleId || contextModule?.id || null
        }),
        signal: abortController.signal
      });

      if (response.status === 429) {
        setRateLimitBanner(true);
        setIsStreaming(false);
        setMessages((prev) => prev.filter((m) => m.id !== assistantMsgId));
        return;
      }

      if (!response.ok) {
        throw new Error(`Server responded with status: ${response.status}`);
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      let receivedAnyToken = false;

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || ''; // Keep incomplete trailing fragment in buffer

        for (let i = 0; i < lines.length; i++) {
          const line = lines[i].trim();
          if (!line) continue;

          // Check for final event citations
          if (line.startsWith('event: citations') || line.startsWith('event:citations')) {
            const nextLine = lines[i + 1]?.trim() || '';
            if (nextLine.startsWith('data:')) {
              try {
                const citationsData = JSON.parse(nextLine.slice(5).trim());
                setMessages((prev) =>
                  prev.map((msg) =>
                    msg.id === assistantMsgId ? { ...msg, citations: citationsData } : msg
                  )
                );
                i++; // Skip citations data line
              } catch (ignored) {}
            }
          } else if (line.startsWith('data:')) {
            const dataStr = line.slice(5).trim();
            if (dataStr === '[DONE]') {
              break;
            }

            try {
              const parsed = JSON.parse(dataStr);
              if (parsed.token !== undefined) {
                receivedAnyToken = true;
                setMessages((prev) =>
                  prev.map((msg) =>
                    msg.id === assistantMsgId
                      ? { ...msg, content: (msg.content || '') + parsed.token }
                      : msg
                  )
                );
              }
            } catch {
              // Plain text token fallback
              if (dataStr && !dataStr.startsWith('{')) {
                receivedAnyToken = true;
                setMessages((prev) =>
                  prev.map((msg) =>
                    msg.id === assistantMsgId
                      ? { ...msg, content: (msg.content || '') + dataStr }
                      : msg
                  )
                );
              }
            }
          }
        }
      }

      // If finished stream but empty content, provide a safe fallback
      if (!receivedAnyToken) {
        setMessages((prev) =>
          prev.map((msg) =>
            msg.id === assistantMsgId && !msg.content
              ? {
                  ...msg,
                  content: 'I have processed your query. How else can I help deepen your understanding?'
                }
              : msg
          )
        );
      }

    } catch (err) {
      if (err.name === 'AbortError') {
        console.log('User cancelled stream');
      } else {
        console.error('SSE Streaming error:', err);
        setStreamingError('Streaming was interrupted. You can retry sending your message.');
        // Ensure the assistant bubble doesn't look empty and broken
        setMessages((prev) =>
          prev.map((msg) =>
            msg.id === assistantMsgId && !msg.content
              ? {
                  ...msg,
                  content: 'Sorry, I encountered an issue connecting to the chat service. Please click Retry above to resend.',
                  isError: true
                }
              : msg
          )
        );
      }
    } finally {
      setIsStreaming(false);
      abortControllerRef.current = null;
    }
  };

  const handleStopStreaming = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      setIsStreaming(false);
      abortControllerRef.current = null;
    }
  };

  // Copy message text
  const handleCopyMessage = (id, text) => {
    navigator.clipboard.writeText(text);
    setCopiedMsgId(id);
    addToast('Message copied to clipboard', 'info');
    setTimeout(() => setCopiedMsgId(null), 2000);
  };

  // Regenerate last assistant response
  const handleRegenerate = (msgIndex) => {
    let lastUserText = '';
    for (let i = msgIndex - 1; i >= 0; i--) {
      if (messages[i].role === 'USER') {
        lastUserText = messages[i].content;
        break;
      }
    }
    if (lastUserText) {
      handleSendMessage(lastUserText);
    }
  };

  // Save to notes
  const handleSaveToNotes = async (text) => {
    try {
      await axiosInstance.post('/documents/notes', {
        title: 'Chat Insight Note',
        body: text
      });
      addToast('Saved to your study notes!', 'success');
    } catch {
      navigator.clipboard.writeText(text);
      addToast('Insight copied to clipboard (Notes API offline)', 'info');
    }
  };

  // Markdown renderer with syntax blocks and styling
  const renderMarkdown = (text) => {
    if (!text) return null;

    const codeBlockRegex = /```([a-zA-Z0-9_-]*)\n([\s\S]*?)```/g;
    const parts = [];
    let lastIndex = 0;
    let match;

    while ((match = codeBlockRegex.exec(text)) !== null) {
      if (match.index > lastIndex) {
        parts.push({
          type: 'text',
          content: text.slice(lastIndex, match.index)
        });
      }

      parts.push({
        type: 'code',
        language: match[1] || 'code',
        content: match[2].trimEnd()
      });

      lastIndex = match.index + match[0].length;
    }

    if (lastIndex < text.length) {
      parts.push({
        type: 'text',
        content: text.slice(lastIndex)
      });
    }

    return (
      <div className={styles.markdownBody}>
        {parts.map((part, idx) => {
          if (part.type === 'code') {
            return (
              <div key={idx} className={styles.codeBlock}>
                <div className={styles.codeHeader}>
                  <span className={styles.codeLang}>{part.language}</span>
                  <button
                    className={styles.copyCodeBtn}
                    onClick={() => {
                      navigator.clipboard.writeText(part.content);
                      addToast('Code copied to clipboard', 'info');
                    }}
                    title="Copy code"
                  >
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                      <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                    </svg>
                    <span>Copy</span>
                  </button>
                </div>
                <pre>
                  <code>{part.content}</code>
                </pre>
              </div>
            );
          }

          // Plain text markdown (headings, lists, bold)
          const paragraphs = part.content.split('\n\n');
          return paragraphs.map((para, pIdx) => {
            const trimmed = para.trim();
            if (!trimmed) return null;

            if (trimmed.startsWith('#### ')) {
              return <h4 key={`${idx}-${pIdx}`} className={styles.mdH4}>{trimmed.slice(5)}</h4>;
            }
            if (trimmed.startsWith('### ')) {
              return <h3 key={`${idx}-${pIdx}`} className={styles.mdH3}>{trimmed.slice(4)}</h3>;
            }
            if (trimmed.startsWith('## ')) {
              return <h2 key={`${idx}-${pIdx}`} className={styles.mdH2}>{trimmed.slice(3)}</h2>;
            }

            // Bullet lists
            if (trimmed.startsWith('- ') || trimmed.startsWith('* ')) {
              const items = trimmed.split('\n').map((l) => l.replace(/^[-*]\s+/, ''));
              return (
                <ul key={`${idx}-${pIdx}`} className={styles.mdUl}>
                  {items.map((item, liIdx) => (
                    <li key={liIdx}>{renderInlineMarkdown(item)}</li>
                  ))}
                </ul>
              );
            }

            // Numbered lists
            if (/^\d+\.\s/.test(trimmed)) {
              const items = trimmed.split('\n').map((l) => l.replace(/^\d+\.\s+/, ''));
              return (
                <ol key={`${idx}-${pIdx}`} className={styles.mdOl}>
                  {items.map((item, liIdx) => (
                    <li key={liIdx}>{renderInlineMarkdown(item)}</li>
                  ))}
                </ol>
              );
            }

            return <p key={`${idx}-${pIdx}`} className={styles.mdP}>{renderInlineMarkdown(trimmed)}</p>;
          });
        })}
      </div>
    );
  };

  const renderInlineMarkdown = (str) => {
    // Bold **text**
    const parts = str.split(/(\*\*.*?\*\*|`.*?`)/g);
    return parts.map((chunk, i) => {
      if (chunk.startsWith('**') && chunk.endsWith('**')) {
        return <strong key={i}>{chunk.slice(2, -2)}</strong>;
      }
      if (chunk.startsWith('`') && chunk.endsWith('`')) {
        return <code key={i} className={styles.inlineCode}>{chunk.slice(1, -1)}</code>;
      }
      return chunk;
    });
  };

  return (
    <div className={`${styles.threadContainer} ${isDrawer ? styles.drawerVariant : ''} ${className}`}>
      {/* Assessment Lock Banner */}
      {assessmentInProgress && (
        <div className={styles.bannerNotice}>
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
            <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
          </svg>
          <span>Assessment in progress — AI cannot assist with active test questions until submission.</span>
        </div>
      )}

      {/* Rate Limit Banner */}
      {rateLimitBanner && (
        <div className={styles.bannerRateLimit}>
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
          <span>Rate limit reached. Please wait a moment before sending more messages.</span>
        </div>
      )}

      {/* Interrupted Streaming Banner */}
      {streamingError && (
        <div className={styles.bannerError}>
          <div className={styles.bannerErrorLeft}>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
              <line x1="12" y1="9" x2="12" y2="13"></line>
              <line x1="12" y1="17" x2="12.01" y2="17"></line>
            </svg>
            <span>{streamingError}</span>
          </div>
          <button
            className={styles.retryBtn}
            onClick={() => {
              setStreamingError(null);
              const lastUserMsg = [...messages].reverse().find((m) => m.role === 'USER');
              if (lastUserMsg) handleSendMessage(lastUserMsg.content);
            }}
          >
            Retry
          </button>
        </div>
      )}

      {/* Context Chips Bar */}
      <div className={styles.contextBar}>
        <div className={styles.contextChips}>
          <span className={styles.chip} title="Roadmaps created">
            <span className={styles.chipIcon}>🗺️</span>
            <span>{contextMeta.roadmapCount || 0} Roadmap{contextMeta.roadmapCount === 1 ? '' : 's'}</span>
          </span>
          <span className={styles.chip} title="Active study modules">
            <span className={styles.chipIcon}>📦</span>
            <span>{contextMeta.moduleCount || 0} Module{contextMeta.moduleCount === 1 ? '' : 's'}</span>
          </span>
          <span className={styles.chip} title="Uploaded documents in knowledge base">
            <span className={styles.chipIcon}>📄</span>
            <span>{contextMeta.documentCount || 0} Document{contextMeta.documentCount === 1 ? '' : 's'}</span>
          </span>
          {initialModuleId && (
            <span className={`${styles.chip} ${styles.chipActiveModule}`}>
              <span className={styles.chipDot}></span>
              <span>Module Context #{initialModuleId}</span>
            </span>
          )}
        </div>
      </div>

      {/* Message List Area */}
      <div className={styles.messageArea}>
        {loadingHistory ? (
          <div className={styles.historyLoading}>
            <div className={styles.spinner} />
            <span>Loading conversation...</span>
          </div>
        ) : messages.length === 0 ? (
          <div className={styles.emptyState}>
            <div className={styles.emptyIcon}>
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path>
              </svg>
            </div>
            <h3>StudyGen AI Assistant</h3>
            <p>
              Ask conceptual questions, request code walk-throughs, or synthesize your study roadmap topics.
            </p>
            <div className={styles.emptySuggestions}>
              {SUGGESTION_CHIPS.map((chipText, i) => (
                <button
                  key={i}
                  className={styles.suggestionChip}
                  onClick={() => handleSendMessage(chipText)}
                >
                  <span className={styles.suggestionSparkle}>✨</span>
                  <span>{chipText}</span>
                </button>
              ))}
            </div>
          </div>
        ) : (
          messages.map((msg, idx) => {
            const isUser = msg.role === 'USER' || msg.sender === 'USER';
            const isLatestAssistant = !isUser && idx === messages.length - 1 && isStreaming;

            return (
              <div
                key={msg.id || idx}
                className={`${styles.messageRow} ${isUser ? styles.messageRowUser : styles.messageRowAssistant}`}
              >
                <div className={`${styles.avatar} ${isUser ? styles.avatarUser : styles.avatarAssistant}`}>
                  {isUser ? (
                    <span className={styles.avatarInitial}>U</span>
                  ) : (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
                    </svg>
                  )}
                </div>

                <div className={styles.messageContentWrapper}>
                  <div className={isUser ? styles.bubbleUser : styles.bubbleAssistant}>
                    {isUser ? (
                      <div className={styles.userText}>{msg.content || msg.messageText}</div>
                    ) : (
                      <>
                        {renderMarkdown(msg.content || msg.messageText)}
                        {isLatestAssistant && (
                          <span className={styles.typingCursor} aria-hidden="true">▋</span>
                        )}
                      </>
                    )}
                  </div>

                  {/* Message Footer: Citations & Action Buttons */}
                  {!isUser && (msg.content || msg.messageText) && (
                    <div className={styles.messageFooter}>
                      {/* Citation chips appearing once streaming finishes */}
                      {msg.citations && msg.citations.length > 0 && (
                        <div className={styles.citationsContainer}>
                          {msg.citations.map((cit, cIdx) => (
                            <div key={cIdx} className={styles.citationWrap}>
                              <button
                                className={styles.citationChip}
                                onClick={() =>
                                  setActivePopover(activePopover?.id === `${msg.id}-${cIdx}` ? null : {
                                    id: `${msg.id}-${cIdx}`,
                                    ...cit
                                  })
                                }
                              >
                                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                                  <polyline points="14 2 14 8 20 8"></polyline>
                                </svg>
                                <span>{cit.sourceTitle || cit.source || 'Reference'}</span>
                              </button>

                              {/* Citation Popover */}
                              {activePopover?.id === `${msg.id}-${cIdx}` && (
                                <div className={styles.citationPopover}>
                                  <div className={styles.popoverHeader}>
                                    <span>{cit.sourceTitle}</span>
                                    <button
                                      className={styles.popoverClose}
                                      onClick={() => setActivePopover(null)}
                                    >
                                      &times;
                                    </button>
                                  </div>
                                  <p className={styles.popoverSnippet}>{cit.snippet}</p>
                                  {cit.sourceUrl && (
                                    <a
                                      href={cit.sourceUrl}
                                      className={styles.popoverLink}
                                      target="_blank"
                                      rel="noreferrer"
                                    >
                                      Open Source Document &rarr;
                                    </a>
                                  )}
                                </div>
                              )}
                            </div>
                          ))}
                        </div>
                      )}

                      {/* Icon Buttons for Assistant: Copy / Regenerate / Save to Notes */}
                      <div className={styles.messageActions}>
                        <button
                          className={styles.iconBtn}
                          onClick={() => handleCopyMessage(msg.id, msg.content || msg.messageText)}
                          title="Copy text"
                        >
                          {copiedMsgId === msg.id ? (
                            <span className={styles.copiedHint}>✓ Copied</span>
                          ) : (
                            <>
                              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                                <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                              </svg>
                              <span>Copy</span>
                            </>
                          )}
                        </button>

                        <button
                          className={styles.iconBtn}
                          onClick={() => handleRegenerate(idx)}
                          title="Regenerate answer"
                          disabled={isStreaming}
                        >
                          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <polyline points="23 4 23 10 17 10"></polyline>
                            <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
                          </svg>
                          <span>Regenerate</span>
                        </button>

                        <button
                          className={styles.iconBtn}
                          onClick={() => handleSaveToNotes(msg.content || msg.messageText)}
                          title="Save to study notes"
                        >
                          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                            <polyline points="17 21 17 13 7 13 7 21"></polyline>
                            <polyline points="7 3 7 8 15 8"></polyline>
                          </svg>
                          <span>Save to Notes</span>
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            );
          })
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Composer Section */}
      <div className={styles.composerArea}>
        {assessmentInProgress && (
          <div className={styles.assessmentLockNotice}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="8" x2="12" y2="12"></line>
              <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
            <span>AI assistance is restricted while an assessment is active.</span>
          </div>
        )}

        <div className={styles.composerWrapper}>
          <textarea
            ref={textareaRef}
            rows={1}
            value={input}
            onChange={handleTextareaChange}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handleSendMessage();
              }
            }}
            placeholder={
              assessmentInProgress
                ? 'Chat paused during active assessment...'
                : 'Ask a question or explain a concept... (Enter to send, Shift+Enter for new line)'
            }
            className={styles.textarea}
            disabled={assessmentInProgress || isStreaming}
          />

          <div className={styles.composerFooter}>
            <div className={styles.composerMeta}>
              <span className={`${styles.charCount} ${input.length >= 1900 ? styles.charCountLimit : ''}`}>
                {input.length} / 2000
              </span>
              <span className={styles.shortcutHint}>Press <strong>↵</strong> to send</span>
            </div>

            <div className={styles.composerActions}>
              {isStreaming ? (
                <button
                  type="button"
                  onClick={handleStopStreaming}
                  className={styles.stopBtn}
                  title="Stop generating"
                >
                  <span className={styles.stopSquare}>■</span>
                  <span>Stop</span>
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => handleSendMessage()}
                  disabled={!input.trim() || assessmentInProgress}
                  className={styles.sendBtn}
                  title="Send message"
                >
                  <span>Send</span>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                    <line x1="12" y1="19" x2="12" y2="5"></line>
                    <polyline points="5 12 12 5 19 12"></polyline>
                  </svg>
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ChatThread;
