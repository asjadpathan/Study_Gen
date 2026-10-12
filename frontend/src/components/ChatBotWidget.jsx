import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import axiosInstance from '../services/axiosInstance';
import ChatThread from './ChatThread';
import styles from './ChatBotWidget.module.css';

const ChatBotWidget = ({ currentModuleId }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [botSessionId, setBotSessionId] = useState(null);
  const [moduleTitle, setModuleTitle] = useState(null);
  const location = useLocation();
  const navigate = useNavigate();

  // Hide the floating widget if the user is already on the dedicated full-screen AI Chat page
  const isFullChatPage = location.pathname === '/chat';

  // Fetch module title if module ID is active
  useEffect(() => {
    let isMounted = true;
    if (isOpen && currentModuleId) {
      axiosInstance
        .get(`/modules/${currentModuleId}`)
        .then((res) => {
          if (isMounted && res.data?.title) {
            setModuleTitle(res.data.title);
          }
        })
        .catch(() => {
          if (isMounted) setModuleTitle(null);
        });
    } else {
      setModuleTitle(null);
    }
    return () => {
      isMounted = false;
    };
  }, [isOpen, currentModuleId]);

  // Initialize or fetch session when widget is opened
  useEffect(() => {
    let isMounted = true;
    if (isOpen && !botSessionId) {
      const initSession = async () => {
        try {
          const sessionsRes = await axiosInstance.get('/chat/sessions');
          const sessions = sessionsRes.data || [];
          let target = null;
          if (currentModuleId) {
            target = sessions.find(
              (s) => s.moduleId === currentModuleId && s.contextTag === 'MODULE'
            );
          } else {
            target = sessions.find((s) => s.contextTag === 'GENERAL') || sessions[0];
          }

          if (target && isMounted) {
            setBotSessionId(target.id);
          } else if (isMounted) {
            const createRes = await axiosInstance.post('/chat/sessions', {
              title: currentModuleId ? `Module: ${moduleTitle || 'Study'}` : 'AI Study Assistant',
              contextTag: currentModuleId ? 'MODULE' : 'GENERAL',
              moduleId: currentModuleId || null,
            });
            if (isMounted) {
              setBotSessionId(createRes.data.id);
            }
          }
        } catch (err) {
          console.error('Failed to init chatbot session', err);
        }
      };
      initSession();
    }
  }, [isOpen, currentModuleId, botSessionId, moduleTitle]);

  if (isFullChatPage) {
    return null;
  }

  const handleOpenFullChat = () => {
    setIsOpen(false);
    navigate('/chat');
  };

  return (
    <div className={styles.chatbotContainer}>
      {/* Floating Chat Window */}
      {isOpen && (
        <div className={styles.chatWindow} role="dialog" aria-label="StudyGen AI Chatbot">
          {/* Header */}
          <div className={styles.windowHeader}>
            <div className={styles.headerInfo}>
              <div className={styles.avatarWrapper}>
                <span className={styles.avatarIcon}>🤖</span>
                <span className={styles.onlineDot} title="Online" />
              </div>
              <div className={styles.titleColumn}>
                <div className={styles.titleRow}>
                  <h3 className={styles.windowTitle}>StudyGen AI</h3>
                  <span className={styles.badgePill}>Bot</span>
                </div>
                <p className={styles.windowSubtitle}>
                  {moduleTitle ? `Context: ${moduleTitle}` : 'Ask questions, get instant answers'}
                </p>
              </div>
            </div>

            <div className={styles.headerActions}>
              <button
                type="button"
                className={styles.headerBtn}
                onClick={handleOpenFullChat}
                title="Open full AI Chat page"
                aria-label="Open in full chat"
              >
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
                  <polyline points="15 3 21 3 21 9" />
                  <line x1="10" y1="14" x2="21" y2="3" />
                </svg>
              </button>
              <button
                type="button"
                className={styles.headerBtn}
                onClick={() => setIsOpen(false)}
                title="Minimize chatbot"
                aria-label="Minimize chatbot"
              >
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>
          </div>

          {/* Chat Body */}
          <div className={styles.windowBody}>
            <ChatThread
              sessionId={botSessionId}
              isDrawer={true}
              initialModuleId={currentModuleId}
              onSessionCreated={(newId) => setBotSessionId(newId)}
            />
          </div>
        </div>
      )}

      {/* Floating Trigger Button in Right Corner */}
      <button
        type="button"
        className={`${styles.floatingTrigger} ${isOpen ? styles.triggerActive : ''}`}
        onClick={() => setIsOpen(!isOpen)}
        title={isOpen ? 'Close AI Chatbot' : 'Ask StudyGen AI'}
        aria-label="Toggle AI Chatbot"
      >
        <span className={styles.triggerPulse} />
        {isOpen ? (
          <span className={styles.closeIcon}>✕</span>
        ) : (
          <>
            <span className={styles.botIcon}>🤖</span>
            <span className={styles.triggerTooltip}>Ask AI</span>
          </>
        )}
      </button>
    </div>
  );
};

export default ChatBotWidget;
