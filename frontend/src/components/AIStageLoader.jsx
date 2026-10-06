import React, { useState, useEffect } from 'react';
import styles from './AIStageLoader.module.css';

const AIStageLoader = ({
  messages = [
    'Analyzing target topic & requirements...',
    'Synthesizing optimal learning modules...',
    'Generating concept dependency graphs...',
    'Calibrating diagnostic assessment questions...',
    'Finalizing personalized study path...',
  ],
  title = 'AI is Generating Your Roadmap',
  intervalMs = 3000,
}) => {
  const [currentIndex, setCurrentIndex] = useState(0);

  useEffect(() => {
    if (!messages || messages.length <= 1) return;

    const timer = setInterval(() => {
      setCurrentIndex((prev) => (prev + 1) % messages.length);
    }, intervalMs);

    return () => clearInterval(timer);
  }, [messages, intervalMs]);

  return (
    <div className={styles.loaderContainer}>
      <div className={styles.aiOrb}>
        <div className={styles.innerPulse}></div>
        <span className={styles.sparkleIcon}>✨</span>
      </div>
      <h3 className={styles.loaderTitle}>{title}</h3>
      <p key={currentIndex} className={styles.loaderMessage}>
        {messages[currentIndex] || 'Processing...'}
      </p>
      <div className={styles.progressBar}>
        <div
          className={styles.progressFill}
          style={{ width: `${((currentIndex + 1) / messages.length) * 100}%` }}
        ></div>
      </div>
    </div>
  );
};

export default AIStageLoader;
