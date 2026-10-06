import React, { useState, useEffect } from 'react';
import PomodoroTimer from '../components/studyroom/PomodoroTimer';
import AmbientAudioPlayer from '../components/studyroom/AmbientAudioPlayer';
import ProductivityAnalytics from '../components/studyroom/ProductivityAnalytics';
import styles from './StudyRoom.module.css';

export default function StudyRoom() {
  const [isFocusMode, setIsFocusMode] = useState(false);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  // Esc key listener to exit Focus Mode
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isFocusMode) {
        setIsFocusMode(false);
      }
    };

    if (isFocusMode) {
      window.addEventListener('keydown', handleKeyDown);
      // Prevent body scroll when in focus overlay
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [isFocusMode]);

  const handleSessionLogged = () => {
    // Increment trigger to refresh productivity charts immediately
    setRefreshTrigger((prev) => prev + 1);
  };

  return (
    <div className={styles.container}>
      {/* Top Banner Header */}
      <header className={styles.header}>
        <div className={styles.headerContent}>
          <div className={styles.badgeRow}>
            <span className={styles.roomBadge}>
              <span className={styles.pulseDot}></span> Flow Zone
            </span>
          </div>
          <h1 className={styles.title}>Study Room & Deep Focus</h1>
          <p className={styles.subtitle}>
            Eliminate distractions with timed Pomodoro sprints, curated ambient soundscapes, and
            real-time productivity tracking.
          </p>
        </div>

        <div className={styles.headerActions}>
          <button
            className={styles.focusModeButton}
            onClick={() => setIsFocusMode(true)}
            title="Enter distraction-free full-viewport mode"
          >
            <svg
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <circle cx="12" cy="12" r="10"></circle>
              <circle cx="12" cy="12" r="6"></circle>
              <circle cx="12" cy="12" r="2"></circle>
            </svg>
            <span>Focus Mode</span>
            <kbd className={styles.shortcutKey}>Esc</kbd>
          </button>
        </div>
      </header>

      {/* Main Two-Column Grid */}
      <div className={styles.roomGrid}>
        {/* Left Column: Pomodoro & Audio Player */}
        <div className={styles.controlColumn}>
          <section aria-label="Pomodoro Timer" className={styles.sectionBlock}>
            <PomodoroTimer onSessionLogged={handleSessionLogged} />
          </section>

          <section aria-label="Ambient Soundscape" className={styles.sectionBlock}>
            <AmbientAudioPlayer />
          </section>
        </div>

        {/* Right Column: Live Productivity Analytics */}
        <div className={styles.analyticsColumn}>
          <section aria-label="Productivity Analytics" className={styles.sectionBlock}>
            <ProductivityAnalytics refreshTrigger={refreshTrigger} />
          </section>
        </div>
      </div>

      {/* Full-viewport Focus Mode Overlay */}
      {isFocusMode && (
        <div className={styles.focusOverlay} role="dialog" aria-modal="true">
          {/* Top Bar inside Focus Overlay */}
          <div className={styles.focusTopBar}>
            <div className={styles.focusBrand}>
              <span className={styles.focusBrandIcon}>🧘</span>
              <div>
                <span className={styles.focusBrandTitle}>StudyGen Deep Flow</span>
                <span className={styles.focusBrandSub}>Distraction-free environment</span>
              </div>
            </div>

            <button
              className={styles.exitFocusBtn}
              onClick={() => setIsFocusMode(false)}
              title="Exit Focus Mode (Esc)"
            >
              <span>Exit Focus Mode</span>
              <kbd className={styles.escBadge}>Esc</kbd>
            </button>
          </div>

          {/* Centered Focus Mode Container */}
          <div className={styles.focusCenterWrapper}>
            <div className={styles.focusCardStack}>
              <PomodoroTimer
                isFocusMode={true}
                onSessionLogged={handleSessionLogged}
              />
              <AmbientAudioPlayer isFocusMode={true} />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
