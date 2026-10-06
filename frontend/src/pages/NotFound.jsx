import React from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../components/Button';
import styles from './NotFound.module.css';

const NotFound = () => {
  const navigate = useNavigate();

  return (
    <div className={styles.container}>
      <div className={styles.card} role="region" aria-label="Page Not Found">
        <div className={styles.badge404}>404</div>
        <div className={styles.icon}>🧭</div>
        <h1 className={styles.title}>We can't find that page</h1>
        <p className={styles.description}>
          The link you followed might be broken, or the page may have been moved or removed.
          Let's get you back to your learning dashboard.
        </p>

        <Button
          variant="primary"
          size="lg"
          className={styles.homeBtn}
          onClick={() => navigate('/dashboard')}
        >
          📊 Return to Dashboard
        </Button>
      </div>
    </div>
  );
};

export default NotFound;
