import React from 'react';
import styles from './DifficultyTag.module.css';

const DifficultyTag = ({ level, className = '' }) => {
  const levelUpper = (level || 'BEGINNER').toUpperCase();
  let badgeClass = styles.diffBeginner;
  if (levelUpper === 'INTERMEDIATE') badgeClass = styles.diffIntermediate;
  if (levelUpper === 'ADVANCED') badgeClass = styles.diffAdvanced;

  return (
    <span className={`${styles.diffTag} ${badgeClass} ${className}`}>
      {levelUpper}
    </span>
  );
};

export default DifficultyTag;
