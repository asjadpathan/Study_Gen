import React, { useState } from 'react';
import Card from './Card';
import styles from './QuestionReviewAccordion.module.css';

const optionLetters = ['A', 'B', 'C', 'D'];

const QuestionReviewAccordion = ({ questionsReview = [] }) => {
  const [openIndex, setOpenIndex] = useState(null);

  if (!questionsReview || questionsReview.length === 0) return null;

  const toggleItem = (idx) => {
    setOpenIndex((prev) => (prev === idx ? null : idx));
  };

  return (
    <div className={styles.container}>
      <h3 className={styles.sectionTitle}>Detailed Question Review</h3>

      <div className={styles.accordionList}>
        {questionsReview.map((item, idx) => {
          const isOpen = openIndex === idx;
          const isCorrect = item.isCorrect;

          return (
            <Card
              key={item.questionId || idx}
              className={`${styles.itemCard} ${isCorrect ? styles.correctCard : styles.incorrectCard}`}
            >
              <button
                type="button"
                className={styles.headerBtn}
                onClick={() => toggleItem(idx)}
              >
                <div className={styles.headerLeft}>
                  <span
                    className={`${styles.statusIcon} ${
                      isCorrect ? styles.iconCorrect : styles.iconIncorrect
                    }`}
                  >
                    {isCorrect ? '✓' : '✕'}
                  </span>
                  <span className={styles.questionSummary}>
                    Q{idx + 1}: {item.questionText}
                  </span>
                </div>
                <span className={styles.toggleArrow}>{isOpen ? '▲' : '▼'}</span>
              </button>

              {isOpen && (
                <div className={styles.body}>
                  <div className={styles.optionsList}>
                    {item.options?.map((opt, optIdx) => {
                      const isUserChoice = item.userAnswerIndex === optIdx;
                      const isCorrectChoice = item.correctAnswerIndex === optIdx;

                      let optClass = styles.optionNormal;
                      if (isCorrectChoice) optClass = styles.optionCorrect;
                      else if (isUserChoice && !isCorrect) optClass = styles.optionWrong;

                      return (
                        <div key={optIdx} className={`${styles.optionRow} ${optClass}`}>
                          <span className={styles.letter}>{optionLetters[optIdx]}</span>
                          <span className={styles.label}>{opt}</span>
                          {isUserChoice && (
                            <span className={styles.badgeYourChoice}>Your answer</span>
                          )}
                          {isCorrectChoice && (
                            <span className={styles.badgeCorrect}>Correct</span>
                          )}
                        </div>
                      );
                    })}
                  </div>

                  {item.explanation && (
                    <div className={styles.explanationBox}>
                      <span className={styles.explanationTitle}>💡 Explanation:</span>
                      <p className={styles.explanationText}>{item.explanation}</p>
                    </div>
                  )}
                </div>
              )}
            </Card>
          );
        })}
      </div>
    </div>
  );
};

export default QuestionReviewAccordion;
