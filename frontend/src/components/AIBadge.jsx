import React from 'react';
import styles from './AIBadge.module.css';

const AIBadge = ({ label = 'AI Generated', className = '' }) => {
  return (
    <span className={`${styles.aiBadge} ${className}`}>
      <span className={styles.sparkle}>✨</span>
      {label}
    </span>
  );
};

export default AIBadge;
