import React from 'react';
import { useNetworkStatus } from '../context/NetworkStatusContext';
import styles from './OfflineBanner.module.css';

const OfflineBanner = () => {
  const { isOffline, wasOffline } = useNetworkStatus();

  // If online and was not previously offline during this session, do not render
  if (!isOffline && !wasOffline) {
    return null;
  }

  if (isOffline) {
    return (
      <div className={`${styles.banner} ${styles.offlineState}`} role="alert">
        <span className={styles.icon}>📡</span>
        <span className={styles.text}>
          You are currently offline. Changes will not be saved and forms cannot be submitted.
        </span>
      </div>
    );
  }

  // If just reconnected (wasOffline === true)
  return (
    <div className={`${styles.banner} ${styles.reconnectedState}`} role="status">
      <span className={styles.icon}>🟢</span>
      <span className={styles.text}>
        Connection restored. You are back online!
      </span>
    </div>
  );
};

export default OfflineBanner;
