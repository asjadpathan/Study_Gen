import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import Button from '../components/Button';
import styles from './Locked.module.css';

const Locked = ({
  title = 'Access Restricted: Module Locked',
  description = 'This technical module is currently locked. Complete the preceding roadmap modules and diagnostic assessments to unlock this content.',
  roadmapId,
}) => {
  const navigate = useNavigate();
  const location = useLocation();

  // If state was passed via location.state
  const stateTitle = location.state?.title || title;
  const stateDescription = location.state?.description || description;
  const targetRoadmapId = location.state?.roadmapId || roadmapId;

  return (
    <div className={styles.container}>
      <div className={styles.card} role="region" aria-label="Access Restricted">
        <div className={styles.lockGlow}>🔒</div>
        <span className={styles.statusPill}>HTTP 403 Forbidden</span>
        <h1 className={styles.title}>{stateTitle}</h1>
        <p className={styles.description}>{stateDescription}</p>

        <div className={styles.actions}>
          <Button
            variant="primary"
            size="md"
            className={styles.primaryBtn}
            onClick={() =>
              targetRoadmapId
                ? navigate(`/roadmaps/${targetRoadmapId}`)
                : navigate('/roadmaps')
            }
          >
            📚 View Roadmap
          </Button>

          <Button
            variant="outlined"
            size="md"
            className={styles.secondaryBtn}
            onClick={() => navigate('/dashboard')}
          >
            📊 Go to Dashboard
          </Button>
        </div>
      </div>
    </div>
  );
};

export default Locked;
