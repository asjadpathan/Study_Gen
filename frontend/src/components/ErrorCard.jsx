import React from 'react';
import Button from './Button';
import styles from './ErrorCard.module.css';

const ErrorCard = ({
  status, // 500 | 502 | 503 | 'server' | 'gateway'
  title,
  message,
  onRetry,
  onReload = () => window.location.reload(),
  className = '',
}) => {
  const is502or503 = status === 502 || status === 503 || status === 'gateway';
  const is500orServer = status === 500 || status === 'server' || (!is502or503 && !onRetry);

  // Derive title
  const resolvedTitle =
    title ||
    (is502or503
      ? status === 502
        ? 'Bad Gateway (502)'
        : 'Service Temporarily Unavailable (503)'
      : is500orServer
      ? 'Internal Server Error (500)'
      : 'Something went wrong');

  // Derive message
  const resolvedMessage =
    message ||
    (is502or503
      ? 'Our upstream AI and calculation services are temporarily unreachable. Please retry your request.'
      : is500orServer
      ? 'An unexpected error occurred while processing your request. Please reload the page to restore state.'
      : 'An unexpected issue occurred. Please try again or refresh the page.');

  const icon = is502or503 ? '🌐' : is500orServer ? '⚙️' : '⚠️';

  return (
    <div className={`${styles.errorCard} ${className}`} role="alert">
      <div className={styles.iconCircle}>{icon}</div>
      {status && <span className={styles.statusBadge}>Error {status}</span>}
      <h3 className={styles.title}>{resolvedTitle}</h3>
      <p className={styles.message}>{resolvedMessage}</p>

      <div className={styles.actionsGroup}>
        {/* 502/503: Primary action is Retry */}
        {onRetry && (
          <Button
            variant="primary"
            size="md"
            onClick={onRetry}
            className={styles.retryBtn}
          >
            🔄 Try Again
          </Button>
        )}

        {/* 500 / Generic: Primary action is Reload */}
        {(is500orServer || !onRetry) && (
          <Button
            variant="primary"
            size="md"
            onClick={onReload}
            className={styles.reloadBtn}
          >
            ↻ Reload Page
          </Button>
        )}

        {/* If retry exists, also offer reload as secondary option */}
        {onRetry && (
          <Button
            variant="outlined"
            size="md"
            onClick={onReload}
            className={styles.secondaryBtn}
          >
            ↻ Reload
          </Button>
        )}
      </div>
    </div>
  );
};

export default ErrorCard;
