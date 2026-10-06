import React from 'react';
import Button from './Button';
import styles from './EmptyState.module.css';

const EmptyState = ({
  icon = '📚',
  title = 'No items found',
  description = 'You have not created or generated any items yet.',
  actionLabel,
  onAction,
  className = '',
}) => {
  return (
    <div className={`${styles.emptyState} ${className}`}>
      <div className={styles.iconWrapper}>{icon}</div>
      <h3 className={styles.title}>{title}</h3>
      <p className={styles.description}>{description}</p>
      {actionLabel && onAction && (
        <Button variant="primary" size="md" onClick={onAction} className={styles.actionBtn}>
          {actionLabel}
        </Button>
      )}
    </div>
  );
};

export default EmptyState;
