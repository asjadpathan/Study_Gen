import React, { useState, useEffect } from 'react';
import axiosInstance from '../services/axiosInstance';
import ChatThread from './ChatThread';
import styles from './AIAssistantDrawer.module.css';

const AIAssistantDrawer = ({ isOpen, onClose, currentModuleId }) => {
  const [drawerSessionId, setDrawerSessionId] = useState(null);
  const [moduleTitle, setModuleTitle] = useState(null);

  // When drawer opens, if currentModuleId is provided, optionally fetch module title
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
          // Non-critical, fallback to module ID
          if (isMounted) setModuleTitle(null);
        });
    } else {
      setModuleTitle(null);
    }
    return () => {
      isMounted = false;
    };
  }, [isOpen, currentModuleId]);

  // When opened, find or initialize a drawer session
  useEffect(() => {
    let isMounted = true;
    if (isOpen && !drawerSessionId) {
      const initSession = async () => {
        try {
          // If currentModuleId exists, look for a session for this module, or create one
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
            setDrawerSessionId(target.id);
          } else if (isMounted) {
            const createRes = await axiosInstance.post('/chat/sessions', {
              title: currentModuleId ? `Module: ${moduleTitle || 'Study'}` : 'Quick Assistant',
              contextTag: currentModuleId ? 'MODULE' : 'GENERAL',
              moduleId: currentModuleId || null,
            });
            if (isMounted) {
              setDrawerSessionId(createRes.data.id);
            }
          }
        } catch (err) {
          console.error('Failed to init drawer session', err);
        }
      };
      initSession();
    }
  }, [isOpen, currentModuleId, drawerSessionId, moduleTitle]);

  return (
    <>
      {/* Backdrop overlay */}
      {isOpen && <div className={styles.backdrop} onClick={onClose} />}

      {/* Slide-over Drawer Panel */}
      <aside
        className={`${styles.drawer} ${isOpen ? styles.drawerOpen : ''}`}
        aria-hidden={!isOpen}
      >
        <div className={styles.header}>
          <div className={styles.titleInfo}>
            <span className={styles.sparkleIcon}>⚡</span>
            <div>
              <div className={styles.titleRow}>
                <h3 className={styles.title}>AI Study Assistant</h3>
                <span className={styles.statusOnline}>● Online</span>
              </div>
              {currentModuleId && (
                <div className={styles.moduleBadge}>
                  <span className={styles.moduleBadgeDot}></span>
                  <span>{moduleTitle ? `Module: ${moduleTitle}` : `Module #${currentModuleId}`}</span>
                </div>
              )}
            </div>
          </div>
          <button className={styles.closeBtn} onClick={onClose} aria-label="Close assistant">
            &times;
          </button>
        </div>

        {/* Reusable ChatThread embedded directly in Drawer */}
        <div className={styles.threadWrapper}>
          <ChatThread
            sessionId={drawerSessionId}
            isDrawer={true}
            initialModuleId={currentModuleId}
            onSessionCreated={(newId) => setDrawerSessionId(newId)}
          />
        </div>
      </aside>
    </>
  );
};

export default AIAssistantDrawer;
