import React from 'react';
import styles from './ResultBanner.module.css';

const ResultBanner = ({ score = 0, passingScore = 80 }) => {
  const isPassed = score >= passingScore;
  const clampedScore = Math.min(Math.max(score, 0), 100);

  return (
    <div className={`${styles.bannerContainer} ${isPassed ? styles.passedBanner : styles.failedBanner}`}>
      <div className={styles.topRow}>
        <div className={styles.scoreGroup}>
          <span className={styles.scoreNumber}>{score}%</span>
          <span className={styles.scoreSubtext}>Final Score</span>
        </div>

        <div className={styles.badgeGroup}>
          <span className={`${styles.statusBadge} ${isPassed ? styles.badgePassed : styles.badgeFailed}`}>
            {isPassed ? '✓ PASSED' : '✕ NOT PASSED'}
          </span>
          <span className={styles.passRequirementText}>
            Pass mark: {passingScore}%
          </span>
        </div>
      </div>

      {/* Progress Track with 80% Threshold Vertical Line */}
      <div className={styles.trackContainer}>
        <div className={styles.track}>
          <div
            className={`${styles.fill} ${isPassed ? styles.fillPassed : styles.fillFailed}`}
            style={{ width: `${clampedScore}%` }}
          />
          {/* Vertical 80% Threshold Marker */}
          <div
            className={styles.thresholdMarker}
            style={{ left: `${passingScore}%` }}
            title={`Passing threshold: ${passingScore}%`}
          >
            <span className={styles.markerLabel}>{passingScore}%</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ResultBanner;
