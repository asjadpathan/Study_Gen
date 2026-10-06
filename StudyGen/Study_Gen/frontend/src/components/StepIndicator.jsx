import React from 'react';
import styles from './StepIndicator.module.css';

const StepIndicator = ({ steps = [], currentStep = 1, onStepClick }) => {
  return (
    <div className={styles.container}>
      {steps.map((step, idx) => {
        const stepNum = idx + 1;
        const isCompleted = stepNum < currentStep;
        const isCurrent = stepNum === currentStep;

        let statusClass = styles.upcoming;
        if (isCompleted) statusClass = styles.completed;
        if (isCurrent) statusClass = styles.current;

        return (
          <React.Fragment key={stepNum}>
            <div
              className={`${styles.stepWrapper} ${statusClass} ${
                onStepClick && isCompleted ? styles.clickable : ''
              }`}
              onClick={() => {
                if (onStepClick && isCompleted) onStepClick(stepNum);
              }}
            >
              <div className={styles.circle}>
                {isCompleted ? '✓' : stepNum}
              </div>
              <div className={styles.labelWrapper}>
                <span className={styles.stepTitle}>Step {stepNum}</span>
                <span className={styles.stepLabel}>{step.title}</span>
              </div>
            </div>

            {idx < steps.length - 1 && (
              <div
                className={`${styles.line} ${
                  stepNum < currentStep ? styles.lineCompleted : ''
                }`}
              />
            )}
          </React.Fragment>
        );
      })}
    </div>
  );
};

export default StepIndicator;
