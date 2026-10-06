import React, { useState, useEffect } from 'react';
import styles from './RateLimitBanner.module.css';

/**
 * Helper to parse Retry-After header:
 * Can be either a number of seconds or a HTTP-date string.
 */
export const parseRetryAfter = (retryAfter) => {
  if (!retryAfter) return 30; // sensible default
  if (typeof retryAfter === 'number') return Math.max(1, retryAfter);

  const parsedInt = parseInt(retryAfter, 10);
  if (!isNaN(parsedInt)) {
    return Math.max(1, parsedInt);
  }

  const parsedDate = new Date(retryAfter).getTime();
  if (!isNaN(parsedDate)) {
    const diffSeconds = Math.ceil((parsedDate - Date.now()) / 1000);
    return Math.max(1, diffSeconds);
  }

  return 30;
};

const RateLimitBanner = ({
  retryAfter,
  retryAfterSeconds,
  message = 'AI generation and request quota limit reached.',
  onDismiss,
}) => {
  const initialSeconds = retryAfterSeconds || parseRetryAfter(retryAfter);
  const [secondsRemaining, setSecondsRemaining] = useState(initialSeconds);
  const [visible, setVisible] = useState(true);

  // Sync initial seconds if props change
  useEffect(() => {
    const sec = retryAfterSeconds || parseRetryAfter(retryAfter);
    setSecondsRemaining(sec);
    setVisible(true);
  }, [retryAfter, retryAfterSeconds]);

  // Plain setInterval-driven countdown
  useEffect(() => {
    if (!visible || secondsRemaining <= 0) return;

    const interval = setInterval(() => {
      setSecondsRemaining((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          setVisible(false);
          if (onDismiss) onDismiss();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [visible, secondsRemaining, onDismiss]);

  if (!visible || secondsRemaining <= 0) {
    return null;
  }

  return (
    <div className={styles.fixedBanner} role="alert">
      <div className={styles.content}>
        <span className={styles.icon}>⏱️</span>
        <div>
          <h4 className={styles.title}>Rate Limit Exceeded (429)</h4>
          <p className={styles.message}>
            {message} Retry available in
            <span className={styles.countdownBadge}>{secondsRemaining}s</span>
          </p>
        </div>
      </div>
      <button
        type="button"
        className={styles.dismissBtn}
        onClick={() => {
          setVisible(false);
          if (onDismiss) onDismiss();
        }}
        aria-label="Dismiss rate limit banner"
      >
        Dismiss
      </button>
    </div>
  );
};

export default RateLimitBanner;
