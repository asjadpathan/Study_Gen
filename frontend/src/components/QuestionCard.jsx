import React from 'react';
import Card from './Card';
import styles from './QuestionCard.module.css';

const optionLetters = ['A', 'B', 'C', 'D'];

const QuestionCard = ({
  question,
  currentIndex = 0,
  totalQuestions = 1,
  selectedAnswer = null,
  onSelectAnswer,
}) => {
  if (!question) return null;

  return (
    <Card className={styles.card}>
      <div className={styles.header}>
        <span className={styles.stepBadge}>
          Question {currentIndex + 1} of {totalQuestions}
        </span>
      </div>

      <h2 className={styles.questionText}>{question.questionText}</h2>

      <div className={styles.optionsList}>
        {question.options?.map((option, idx) => {
          const isSelected = selectedAnswer === idx;
          return (
            <button
              key={idx}
              type="button"
              className={`${styles.optionBtn} ${isSelected ? styles.selectedOption : ''}`}
              onClick={() => onSelectAnswer(idx)}
            >
              <span className={styles.letterBadge}>{optionLetters[idx] || idx + 1}</span>
              <span className={styles.optionLabel}>{option}</span>
              <div className={styles.radioCircle}>
                {isSelected && <div className={styles.radioDot} />}
              </div>
            </button>
          );
        })}
      </div>
    </Card>
  );
};

export default QuestionCard;
