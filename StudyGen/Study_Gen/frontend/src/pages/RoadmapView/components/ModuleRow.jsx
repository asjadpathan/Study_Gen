import React from 'react';
import { useNavigate } from 'react-router-dom';
import StatusPill from '../../../components/StatusPill';
import Button from '../../../components/Button';
import styles from './ModuleRow.module.css';

const DifficultyTag = ({ level }) => {
  const levelUpper = (level || 'BEGINNER').toUpperCase();
  let badgeClass = styles.diffBeginner;
  if (levelUpper === 'INTERMEDIATE') badgeClass = styles.diffIntermediate;
  if (levelUpper === 'ADVANCED') badgeClass = styles.diffAdvanced;

  return <span className={`${styles.diffTag} ${badgeClass}`}>{levelUpper}</span>;
};

const ModuleRow = ({ module, disabled = false }) => {
  const navigate = useNavigate();

  const isLocked = module.isLocked || module.statusBadge === 'LOCKED';
  const isCompleted = module.isCompleted || module.statusBadge === 'COMPLETED';

  const handleAction = () => {
    if (disabled || isLocked) return;
    // Always navigate to the teaching and learning workspace first
    navigate(`/modules/${module.id}`);
  };

  return (
    <div
      className={`${styles.rowCard} ${isLocked ? styles.lockedRow : ''} ${
        isCompleted ? styles.completedRow : ''
      }`}
    >
      {/* Sequence Number */}
      <div className={styles.seqBadge}>{module.sequenceNumber || '01'}</div>

      {/* Main Content */}
      <div className={styles.mainInfo}>
        <div className={styles.titleRow}>
          <h3 className={styles.moduleTitle}>
            {isLocked && <span className={styles.lockIcon}>🔒 </span>}
            {module.title}
          </h3>
          <DifficultyTag level={module.difficulty} />
          <StatusPill status={module.statusBadge || (isCompleted ? 'COMPLETED' : isLocked ? 'LOCKED' : 'UNLOCKED')} />
        </div>

        <p className={styles.description}>{module.description}</p>

        {/* Skill Tags & Meta */}
        <div className={styles.metaRow}>
          {module.skills && module.skills.length > 0 && (
            <div className={styles.skillsGroup}>
              {module.skills.map((skill, idx) => (
                <span key={idx} className={styles.skillChip}>
                  {skill}
                </span>
              ))}
            </div>
          )}

          <div className={styles.duration}>
            ⏱️ {module.estimatedMinutes || 45} mins
          </div>
        </div>
      </div>

      {/* Action Button */}
      <div className={styles.actionCol}>
        {isLocked ? (
          <Button variant="outlined" size="sm" disabled className={styles.lockedBtn}>
            🔒 Locked
          </Button>
        ) : isCompleted ? (
          <Button variant="outlined" size="sm" onClick={handleAction} disabled={disabled}>
            Review
          </Button>
        ) : (
          <Button variant="primary" size="sm" onClick={handleAction} disabled={disabled}>
            Start Module →
          </Button>
        )}
      </div>
    </div>
  );
};

export default ModuleRow;
