import React, { useState, useEffect, useRef, useCallback } from 'react';
import axiosInstance from '../../services/axiosInstance';
import { useToast } from '../../context/ToastContext';
import styles from './PomodoroTimer.module.css';

const PRESETS = [
  { label: '25 / 5', focus: 25, break: 5 },
  { label: '50 / 10', focus: 50, break: 10 },
];

const LOCAL_STORAGE_KEY = 'studygen_pomodoro_state';
const QUEUE_STORAGE_KEY = 'studygen_pomodoro_queue';

export default function PomodoroTimer({ onSessionLogged, isFocusMode = false }) {
  const { addToast } = useToast();

  // Settings
  const [focusDuration, setFocusDuration] = useState(25);
  const [breakDuration, setBreakDuration] = useState(5);
  const [longBreakDuration, setLongBreakDuration] = useState(15);
  const [sessionsBeforeLongBreak] = useState(4);

  // Timer State
  const [phase, setPhase] = useState('FOCUS'); // 'FOCUS' | 'SHORT_BREAK' | 'LONG_BREAK'
  const [timeLeft, setTimeLeft] = useState(25 * 60);
  const [isRunning, setIsRunning] = useState(false);
  const [sessionCount, setSessionCount] = useState(1);

  // Modules for optional tag
  const [modules, setModules] = useState([]);
  const [selectedModuleId, setSelectedModuleId] = useState('');

  // Offline queue state
  const [queuedSessions, setQueuedSessions] = useState([]);
  const [isRetryingQueue, setIsRetryingQueue] = useState(false);

  const timerRef = useRef(null);

  // Load modules for selector
  useEffect(() => {
    const fetchModules = async () => {
      try {
        const res = await axiosInstance.get('/roadmaps');
        const roadmaps = res.data || [];
        const allModules = [];
        for (const rm of roadmaps) {
          if (rm.modules && Array.isArray(rm.modules)) {
            allModules.push(...rm.modules);
          }
        }
        setModules(allModules);
      } catch (err) {
        console.warn('Could not fetch modules for timer selector', err);
      }
    };
    fetchModules();
  }, []);

  // Read offline queue from localStorage
  useEffect(() => {
    try {
      const q = localStorage.getItem(QUEUE_STORAGE_KEY);
      if (q) {
        setQueuedSessions(JSON.parse(q));
      }
    } catch {}
  }, []);

  // 1. Read timer state from localStorage on mount (survives refresh)
  useEffect(() => {
    try {
      const saved = localStorage.getItem(LOCAL_STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved);
        setFocusDuration(parsed.focusDuration || 25);
        setBreakDuration(parsed.breakDuration || 5);
        setLongBreakDuration(parsed.longBreakDuration || 15);
        setPhase(parsed.phase || 'FOCUS');
        setSessionCount(parsed.sessionCount || 1);
        if (parsed.selectedModuleId) setSelectedModuleId(parsed.selectedModuleId);

        // If it was running, calculate elapsed time
        if (parsed.isRunning && parsed.lastSavedAt) {
          const elapsedSecs = Math.floor((Date.now() - parsed.lastSavedAt) / 1000);
          const newTimeLeft = Math.max(0, parsed.timeLeft - elapsedSecs);
          setTimeLeft(newTimeLeft);
          setIsRunning(newTimeLeft > 0);
        } else {
          setTimeLeft(parsed.timeLeft !== undefined ? parsed.timeLeft : 25 * 60);
          setIsRunning(false);
        }
      }
    } catch (err) {
      console.warn('Failed to restore pomodoro state', err);
    }
  }, []);

  // 2. Persist timer state to localStorage whenever it changes
  useEffect(() => {
    try {
      const stateToSave = {
        timeLeft,
        phase,
        isRunning,
        sessionCount,
        focusDuration,
        breakDuration,
        longBreakDuration,
        selectedModuleId,
        lastSavedAt: Date.now(),
      };
      localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(stateToSave));
    } catch {}
  }, [timeLeft, phase, isRunning, sessionCount, focusDuration, breakDuration, longBreakDuration, selectedModuleId]);

  // Log completed focus interval
  const logFocusSession = useCallback(
    async (minutes) => {
      const sessionData = {
        minutes,
        moduleId: selectedModuleId ? parseInt(selectedModuleId, 10) : null,
        timestamp: Date.now(),
      };

      try {
        const res = await axiosInstance.post('/activity/pomodoro', {
          minutes: sessionData.minutes,
          moduleId: sessionData.moduleId,
        });

        const streak = res.data?.streak || 1;
        addToast(`+${minutes} min logged · streak ${streak} 🔥`, 'success');

        if (onSessionLogged) onSessionLogged();
      } catch (err) {
        console.error('Failed to log pomodoro activity', err);
        // Queue in localStorage
        const updatedQueue = [...queuedSessions, sessionData];
        setQueuedSessions(updatedQueue);
        localStorage.setItem(QUEUE_STORAGE_KEY, JSON.stringify(updatedQueue));
        addToast("Couldn't save this session. Retry.", 'error');
      }
    },
    [selectedModuleId, queuedSessions, addToast, onSessionLogged]
  );

  // Retry queued offline sessions
  const handleRetryQueue = async () => {
    if (queuedSessions.length === 0 || isRetryingQueue) return;
    setIsRetryingQueue(true);
    let remainingQueue = [...queuedSessions];

    for (const item of queuedSessions) {
      try {
        await axiosInstance.post('/activity/pomodoro', {
          minutes: item.minutes,
          moduleId: item.moduleId,
        });
        remainingQueue = remainingQueue.filter((q) => q.timestamp !== item.timestamp);
      } catch (err) {
        console.error('Failed to sync queued session', err);
        break;
      }
    }

    setQueuedSessions(remainingQueue);
    localStorage.setItem(QUEUE_STORAGE_KEY, JSON.stringify(remainingQueue));
    setIsRetryingQueue(false);

    if (remainingQueue.length === 0) {
      addToast('All queued study sessions saved successfully!', 'success');
      if (onSessionLogged) onSessionLogged();
    } else {
      addToast('Some sessions could not be synced yet. Will retry later.', 'warning');
    }
  };

  // Timer Tick Loop
  useEffect(() => {
    if (isRunning) {
      timerRef.current = setInterval(() => {
        setTimeLeft((prev) => {
          if (prev <= 1) {
            clearInterval(timerRef.current);
            handlePhaseCompletion();
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    } else {
      clearInterval(timerRef.current);
    }

    return () => clearInterval(timerRef.current);
  }, [isRunning, phase, sessionCount, focusDuration, breakDuration, longBreakDuration]);

  // Phase Transition on Interval Completion
  const handlePhaseCompletion = () => {
    setIsRunning(false);

    if (phase === 'FOCUS') {
      logFocusSession(focusDuration);

      if (sessionCount >= sessionsBeforeLongBreak) {
        setPhase('LONG_BREAK');
        setTimeLeft(longBreakDuration * 60);
        setSessionCount(1);
      } else {
        setPhase('SHORT_BREAK');
        setTimeLeft(breakDuration * 60);
        setSessionCount((prev) => prev + 1);
      }
    } else {
      // Break finished, return to FOCUS
      setPhase('FOCUS');
      setTimeLeft(focusDuration * 60);
      addToast('Break complete! Ready for your next focus session?', 'info');
    }
  };

  // Controls
  const toggleStartPause = () => {
    setIsRunning(!isRunning);
  };

  const handleReset = () => {
    setIsRunning(false);
    if (phase === 'FOCUS') {
      setTimeLeft(focusDuration * 60);
    } else if (phase === 'SHORT_BREAK') {
      setTimeLeft(breakDuration * 60);
    } else {
      setTimeLeft(longBreakDuration * 60);
    }
  };

  const handleSkip = () => {
    setIsRunning(false);
    if (phase === 'FOCUS') {
      setPhase('SHORT_BREAK');
      setTimeLeft(breakDuration * 60);
    } else {
      setPhase('FOCUS');
      setTimeLeft(focusDuration * 60);
    }
  };

  const applyPreset = (focusMins, breakMins) => {
    setIsRunning(false);
    setFocusDuration(focusMins);
    setBreakDuration(breakMins);
    if (phase === 'FOCUS') {
      setTimeLeft(focusMins * 60);
    } else {
      setTimeLeft(breakMins * 60);
    }
  };

  // Format mm:ss
  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  };

  const totalCurrentPhaseSeconds =
    phase === 'FOCUS'
      ? focusDuration * 60
      : phase === 'SHORT_BREAK'
      ? breakDuration * 60
      : longBreakDuration * 60;

  const progressPercent = totalCurrentPhaseSeconds > 0
    ? ((totalCurrentPhaseSeconds - timeLeft) / totalCurrentPhaseSeconds) * 100
    : 0;

  return (
    <div className={`${styles.timerCard} ${isFocusMode ? styles.focusModeCard : ''}`}>
      {/* Offline Queue Notice */}
      {queuedSessions.length > 0 && (
        <div className={styles.queueBanner}>
          <div className={styles.queueBannerLeft}>
            <span className={styles.queueDot}></span>
            <span>{queuedSessions.length} session{queuedSessions.length > 1 ? 's' : ''} queued offline.</span>
          </div>
          <button
            className={styles.queueRetryBtn}
            onClick={handleRetryQueue}
            disabled={isRetryingQueue}
          >
            {isRetryingQueue ? 'Syncing...' : "Couldn't save this session. Retry."}
          </button>
        </div>
      )}

      {/* Phase Label & Session Count */}
      <div className={styles.timerHeader}>
        <div className={`${styles.phaseBadge} ${styles[`phase_${phase.toLowerCase()}`]}`}>
          {phase === 'FOCUS' && '🔥 Focus Session'}
          {phase === 'SHORT_BREAK' && '☕ Short Break'}
          {phase === 'LONG_BREAK' && '🌴 Long Break'}
        </div>
        <div className={styles.sessionCounter}>
          <span>Session {sessionCount} of {sessionsBeforeLongBreak}</span>
          <div className={styles.sessionDots}>
            {[1, 2, 3, 4].map((s) => (
              <span
                key={s}
                className={`${styles.dot} ${s <= sessionCount ? styles.dotFilled : ''}`}
              />
            ))}
          </div>
        </div>
      </div>

      {/* Big Circular / Rounded Countdown */}
      <div className={styles.displayContainer}>
        <div className={styles.progressRingWrapper}>
          <svg className={styles.progressRing} viewBox="0 0 120 120">
            <circle
              className={styles.ringTrack}
              cx="60"
              cy="60"
              r="54"
              strokeWidth="6"
            />
            <circle
              className={styles.ringIndicator}
              cx="60"
              cy="60"
              r="54"
              strokeWidth="6"
              strokeDasharray="339.29"
              strokeDashoffset={339.29 - (339.29 * progressPercent) / 100}
            />
          </svg>
          <div className={styles.countdownValue}>{formatTime(timeLeft)}</div>
        </div>
      </div>

      {/* Main Action Buttons */}
      <div className={styles.controlsRow}>
        <button
          className={`${styles.mainBtn} ${isRunning ? styles.pauseBtn : styles.startBtn}`}
          onClick={toggleStartPause}
        >
          {isRunning ? (
            <>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                <rect x="6" y="4" width="4" height="16" rx="1"></rect>
                <rect x="14" y="4" width="4" height="16" rx="1"></rect>
              </svg>
              <span>Pause</span>
            </>
          ) : (
            <>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                <polygon points="5 3 19 12 5 21 5 3"></polygon>
              </svg>
              <span>Start</span>
            </>
          )}
        </button>

        <button className={styles.secondaryBtn} onClick={handleReset} title="Reset current interval">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
            <polyline points="1 4 1 10 7 10"></polyline>
            <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path>
          </svg>
          <span>Reset</span>
        </button>

        <button className={styles.secondaryBtn} onClick={handleSkip} title="Skip to next phase">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
            <polygon points="5 4 15 12 5 20 5 4"></polygon>
            <line x1="19" y1="5" x2="19" y2="19"></line>
          </svg>
          <span>Skip</span>
        </button>
      </div>

      {/* Presets & Custom Configuration */}
      <div className={styles.configSection}>
        <div className={styles.presetGroup}>
          <span className={styles.configLabel}>Presets:</span>
          {PRESETS.map((p, idx) => (
            <button
              key={idx}
              className={`${styles.presetBtn} ${
                focusDuration === p.focus && breakDuration === p.break ? styles.presetActive : ''
              }`}
              onClick={() => applyPreset(p.focus, p.break)}
            >
              {p.label}
            </button>
          ))}
        </div>

        <div className={styles.customInputGroup}>
          <div className={styles.inputItem}>
            <label>Focus (min)</label>
            <input
              type="number"
              min="1"
              max="120"
              value={focusDuration}
              onChange={(e) => {
                const v = Math.max(1, parseInt(e.target.value) || 1);
                setFocusDuration(v);
                if (phase === 'FOCUS' && !isRunning) setTimeLeft(v * 60);
              }}
            />
          </div>
          <div className={styles.inputItem}>
            <label>Break (min)</label>
            <input
              type="number"
              min="1"
              max="60"
              value={breakDuration}
              onChange={(e) => {
                const v = Math.max(1, parseInt(e.target.value) || 1);
                setBreakDuration(v);
                if (phase === 'SHORT_BREAK' && !isRunning) setTimeLeft(v * 60);
              }}
            />
          </div>
        </div>
      </div>

      {/* Optional Module Selector */}
      <div className={styles.moduleSelectorWrap}>
        <label htmlFor="pomodoro-module-select" className={styles.moduleSelectLabel}>
          <span>🎯 Tag study to module:</span>
        </label>
        <select
          id="pomodoro-module-select"
          className={styles.moduleSelect}
          value={selectedModuleId}
          onChange={(e) => setSelectedModuleId(e.target.value)}
        >
          <option value="">General Study (No specific module)</option>
          {modules.map((m) => (
            <option key={m.id} value={m.id}>
              {m.title || `Module #${m.id}`}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}
