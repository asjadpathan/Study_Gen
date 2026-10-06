import React from 'react';
import styles from './ProgressBar.module.css';

const ProgressBar = ({ progress = 0, showLabel = true, height = '8px', className = '' }) => {
  const percentage = Math.min(100, Math.max(0, Math.round(progress)));

  return (
    <div className={`${styles.progressWrapper} ${className}`}>
      <div className={styles.track} style={{ height }}>
        <div className={styles.fill} style={{ width: `${percentage}%` }} />
      </div>
      {showLabel && <span className={styles.label}>{percentage}%</span>}
    </div>
  );
};

export default ProgressBar;
