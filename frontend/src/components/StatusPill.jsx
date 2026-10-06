import React from 'react';
import styles from './StatusPill.module.css';

const StatusPill = ({ status = 'ACTIVE' }) => {
  const normStatus = (status || 'ACTIVE').toUpperCase();
  const statusClass = styles[normStatus] || styles.ACTIVE;

  return (
    <span className={`${styles.pill} ${statusClass}`}>
      <span className={styles.dot}>●</span>
      {normStatus}
    </span>
  );
};

export default StatusPill;
