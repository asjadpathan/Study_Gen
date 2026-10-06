import React, { useState, useEffect, useCallback } from 'react';
import axiosInstance from '../services/axiosInstance';
import ChatThread from '../components/ChatThread';
import styles from './ChatPage.module.css';

export default function ChatPage() {
  const [sessions, setSessions] = useState([]);
  const [activeSessionId, setActiveSessionId] = useState(null);
  const [loadingSessions, setLoadingSessions] = useState(true);
  const [creatingSession, setCreatingSession] = useState(false);
  const [showMobileList, setShowMobileList] = useState(false);

  // Fetch all sessions
  const fetchSessions = useCallback(async (selectNewest = false) => {
    try {
      const res = await axiosInstance.get('/chat/sessions');
      const list = res.data || [];
      setSessions(list);
      if (list.length > 0) {
        if (selectNewest || !activeSessionId || !list.some((s) => s.id === activeSessionId)) {
          setActiveSessionId(list[0].id);
        }
      } else {
        setActiveSessionId(null);
      }
    } catch (err) {
      console.error('Failed to load chat sessions', err);
    } finally {
      setLoadingSessions(false);
    }
  }, [activeSessionId]);

  useEffect(() => {
    fetchSessions();
  }, []);

  // Create new session
  const handleNewChat = async () => {
    if (creatingSession) return;
    setCreatingSession(true);
    try {
      const res = await axiosInstance.post('/chat/sessions', {
        title: 'New Study Chat',
        contextTag: 'GENERAL',
      });
      const newSession = res.data;
      setSessions((prev) => [newSession, ...prev]);
      setActiveSessionId(newSession.id);
      setShowMobileList(false);
    } catch (err) {
      console.error('Failed to create new session', err);
    } finally {
      setCreatingSession(false);
    }
  };

  // Delete session
  const handleDeleteSession = async (e, sessionId) => {
    e.stopPropagation();
    if (!window.confirm('Are you sure you want to delete this conversation?')) return;
    try {
      await axiosInstance.delete(`/chat/sessions/${sessionId}`);
      setSessions((prev) => prev.filter((s) => s.id !== sessionId));
      if (activeSessionId === sessionId) {
        const remaining = sessions.filter((s) => s.id !== sessionId);
        setActiveSessionId(remaining.length > 0 ? remaining[0].id : null);
      }
    } catch (err) {
      console.error('Failed to delete session', err);
    }
  };

  const formatRelativeTime = (isoString) => {
    if (!isoString) return '';
    try {
      const diffMs = Date.now() - new Date(isoString).getTime();
      const diffMins = Math.floor(diffMs / (1000 * 60));
      const diffHours = Math.floor(diffMins / 60);
      const diffDays = Math.floor(diffHours / 24);

      if (diffMins < 1) return 'Just now';
      if (diffMins < 60) return `${diffMins}m ago`;
      if (diffHours < 24) return `${diffHours}h ago`;
      if (diffDays === 1) return 'Yesterday';
      if (diffDays < 7) return `${diffDays}d ago`;
      return new Date(isoString).toLocaleDateString();
    } catch {
      return '';
    }
  };

  const activeSession = sessions.find((s) => s.id === activeSessionId);

  return (
    <div className={styles.chatPageContainer}>
      <div className={styles.chatLayout}>
        {/* Session List Column */}
        <aside
          className={`${styles.sessionSidebar} ${
            showMobileList ? styles.mobileListVisible : ''
          }`}
        >
          <div className={styles.sidebarHeader}>
            <div className={styles.sidebarTitleWrap}>
              <h2 className={styles.sidebarTitle}>Chats</h2>
              <span className={styles.sessionCount}>{sessions.length}</span>
            </div>
            <button
              className={styles.newChatBtn}
              onClick={handleNewChat}
              disabled={creatingSession}
              title="Start a new chat"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
              <span>New chat</span>
            </button>
          </div>

          <div className={styles.sessionListScroll}>
            {loadingSessions ? (
              <div className={styles.loadingState}>
                <div className={styles.spinner} />
                <span>Loading chats...</span>
              </div>
            ) : sessions.length === 0 ? (
              <div className={styles.emptySessions}>
                <p>No chat sessions yet.</p>
                <button
                  className={styles.emptyCreateBtn}
                  onClick={handleNewChat}
                >
                  Create your first chat
                </button>
              </div>
            ) : (
              sessions.map((session) => {
                const isActive = session.id === activeSessionId;
                return (
                  <div
                    key={session.id}
                    className={`${styles.sessionItem} ${
                      isActive ? styles.sessionItemActive : ''
                    }`}
                    onClick={() => {
                      setActiveSessionId(session.id);
                      setShowMobileList(false);
                    }}
                  >
                    <div className={styles.sessionItemMain}>
                      <div className={styles.sessionTitleRow}>
                        <span className={styles.sessionTitle}>
                          {session.title || 'Study Chat'}
                        </span>
                        {session.contextTag && (
                          <span
                            className={`${styles.contextTag} ${
                              styles[`tag_${session.contextTag.toLowerCase()}`] || ''
                            }`}
                          >
                            {session.contextTag}
                          </span>
                        )}
                      </div>
                      <div className={styles.sessionMetaRow}>
                        <span className={styles.sessionTime}>
                          {formatRelativeTime(session.lastActivityAt || session.createdAt)}
                        </span>
                        <span className={styles.msgCount}>
                          {session.messageCount || 0} msgs
                        </span>
                      </div>
                    </div>
                    <button
                      className={styles.deleteBtn}
                      onClick={(e) => handleDeleteSession(e, session.id)}
                      title="Delete chat"
                    >
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <polyline points="3 6 5 6 21 6"></polyline>
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                      </svg>
                    </button>
                  </div>
                );
              })
            )}
          </div>
        </aside>

        {/* Thread Column */}
        <main className={styles.threadColumn}>
          {/* Mobile bar toggle */}
          <div className={styles.mobileNavHeader}>
            <button
              className={styles.mobileToggleBtn}
              onClick={() => setShowMobileList(!showMobileList)}
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="3" y1="12" x2="21" y2="12"></line>
                <line x1="3" y1="6" x2="21" y2="6"></line>
                <line x1="3" y1="18" x2="21" y2="18"></line>
              </svg>
              <span>{activeSession ? activeSession.title : 'All Chats'}</span>
            </button>
            <button
              className={styles.mobileNewChatBtn}
              onClick={handleNewChat}
              title="New Chat"
            >
              +
            </button>
          </div>

          <div className={styles.threadWrapper}>
            <ChatThread
              sessionId={activeSessionId}
              isDrawer={false}
              onSessionCreated={(newId) => {
                fetchSessions();
                setActiveSessionId(newId);
              }}
            />
          </div>
        </main>
      </div>
    </div>
  );
}
