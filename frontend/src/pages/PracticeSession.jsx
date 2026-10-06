import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import Button from '../components/Button';
import axiosInstance from '../services/axiosInstance';
import styles from './PracticeSession.module.css';

const RATINGS = [
  { quality: 0, label: 'Blackout', desc: 'Complete blank', color: '#ef4444', key: '0' },
  { quality: 1, label: 'Wrong felt familiar', desc: 'Missed, but familiar', color: '#f97316', key: '1' },
  { quality: 2, label: 'Wrong almost', desc: 'Tip of tongue', color: '#f59e0b', key: '2' },
  { quality: 3, label: 'Correct hard', desc: 'Recalled with effort', color: '#84cc16', key: '3' },
  { quality: 4, label: 'Correct', desc: 'Good recall', color: '#10b981', key: '4' },
  { quality: 5, label: 'Perfect', desc: 'Effortless & instant', color: '#059669', key: '5' }
];

const PracticeSession = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const moduleIdParam = searchParams.get('moduleId');

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [cards, setCards] = useState([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isFlipped, setIsFlipped] = useState(false);
  const [showHint, setShowHint] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [sessionResults, setSessionResults] = useState([]);
  const [isFinished, setIsFinished] = useState(false);
  const [showExitConfirm, setShowExitConfirm] = useState(false);

  // Fetch due cards or fallback module cards
  useEffect(() => {
    const fetchSessionCards = async () => {
      setLoading(true);
      setError(null);
      try {
        const res = await axiosInstance.get('/practice/due');
        let availableCards = res.data?.dueCards || [];

        // If filtered by moduleId
        if (moduleIdParam) {
          const filtered = availableCards.filter(
            (c) => String(c.moduleId) === String(moduleIdParam)
          );
          if (filtered.length > 0) {
            availableCards = filtered;
          }
        }

        // If no due cards, fetch module drills as review-ahead cards
        if (availableCards.length === 0 && moduleIdParam) {
          const drillRes = await axiosInstance.get(`/practice/modules/${moduleIdParam}/drills`);
          if (drillRes.data?.flashcards?.length > 0) {
            availableCards = drillRes.data.flashcards.map((fc) => ({
              cardId: fc.conceptId,
              conceptId: fc.conceptId,
              moduleId: Number(moduleIdParam),
              moduleTitle: drillRes.data.moduleTitle || 'Module Review',
              roadmapTitle: 'Active Roadmap',
              front: fc.front,
              back: fc.back,
              hint: fc.analogy || 'Foundational concept',
              repetitionNumber: 0,
              intervalDays: 1,
              easinessFactor: 2.5,
              nextReviewDate: new Date().toISOString().split('T')[0]
            }));
          }
        }

        if (availableCards.length === 0) {
          // If still no cards, try to use any sample deck
          setError('No flashcards are currently available for review.');
        } else {
          setCards(availableCards);
        }
      } catch (err) {
        console.error('Failed to load practice cards:', err);
        setError('Failed to initialize review session. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    fetchSessionCards();
  }, [moduleIdParam]);

  const currentCard = cards[currentIndex] || null;

  // Rating action: submit SM-2 quality and advance
  const handleRate = useCallback(
    async (quality) => {
      if (!currentCard || isSubmitting) return;

      setIsSubmitting(true);
      try {
        const payload = {
          cardId: currentCard.cardId || currentCard.conceptId,
          conceptId: currentCard.conceptId || currentCard.cardId,
          quality,
          qualityRating: quality
        };

        const res = await axiosInstance.post('/practice/review', payload);
        const reviewData = res.data;

        // Record in session results
        setSessionResults((prev) => [
          ...prev,
          {
            cardId: currentCard.cardId,
            front: currentCard.front,
            moduleTitle: currentCard.moduleTitle,
            quality,
            nextReviewDate: reviewData?.nextReviewDate || 'Tomorrow',
            intervalDays: reviewData?.intervalDays || 1
          }
        ]);

        // Advance or Finish
        if (currentIndex + 1 < cards.length) {
          setIsFlipped(false);
          setShowHint(false);
          setCurrentIndex((prev) => prev + 1);
        } else {
          setIsFinished(true);
        }
      } catch (err) {
        console.error('Failed to record card review:', err);
        // Advance anyway so user is not stuck
        setSessionResults((prev) => [
          ...prev,
          {
            cardId: currentCard.cardId,
            front: currentCard.front,
            moduleTitle: currentCard.moduleTitle,
            quality,
            nextReviewDate: 'Tomorrow',
            intervalDays: 1
          }
        ]);
        if (currentIndex + 1 < cards.length) {
          setIsFlipped(false);
          setShowHint(false);
          setCurrentIndex((prev) => prev + 1);
        } else {
          setIsFinished(true);
        }
      } finally {
        setIsSubmitting(false);
      }
    },
    [currentCard, currentIndex, cards.length, isSubmitting]
  );

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e) => {
      // Don't intercept if an input is focused
      if (['INPUT', 'TEXTAREA'].includes(e.target.tagName)) return;

      if (isFinished || loading || error || !currentCard) return;

      if (e.code === 'Space') {
        e.preventDefault();
        setIsFlipped((prev) => !prev);
      } else if (e.key.toLowerCase() === 'h') {
        setShowHint((prev) => !prev);
      } else if (e.key === 'Escape') {
        setShowExitConfirm(true);
      } else if (isFlipped && ['0', '1', '2', '3', '4', '5'].includes(e.key)) {
        e.preventDefault();
        handleRate(Number(e.key));
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isFlipped, isFinished, loading, error, currentCard, handleRate]);

  // Loading State
  if (loading) {
    return (
      <div className={styles.sessionOverlay}>
        <div className={styles.centerLoading}>
          <div className={styles.spinner} />
          <p className={styles.loadingText}>Preparing your spaced repetition deck...</p>
        </div>
      </div>
    );
  }

  // Error State
  if (error || cards.length === 0) {
    return (
      <div className={styles.sessionOverlay}>
        <div className={styles.errorContainer}>
          <div className={styles.errorIcon}>🎴</div>
          <h2 className={styles.errorHeading}>Review Session Ended</h2>
          <p className={styles.errorDesc}>{error || 'No cards due for review.'}</p>
          <div className={styles.errorActions}>
            <Button variant="primary" onClick={() => navigate('/practice')}>
              Back to Practice Hub
            </Button>
            <Button variant="outline" onClick={() => navigate('/dashboard')}>
              Go to Dashboard
            </Button>
          </div>
        </div>
      </div>
    );
  }

  // Session Summary Screen
  if (isFinished) {
    const totalReviewed = sessionResults.length;
    const recalledCount = sessionResults.filter((r) => r.quality >= 3).length;
    const recallRate = totalReviewed > 0 ? Math.round((recalledCount / totalReviewed) * 100) : 100;
    const avgScore =
      totalReviewed > 0
        ? (sessionResults.reduce((acc, r) => acc + r.quality, 0) / totalReviewed).toFixed(1)
        : '5.0';

    return (
      <div className={styles.sessionOverlay}>
        <div className={styles.summaryContainer}>
          <div className={styles.summaryHeader}>
            <div className={styles.summaryTrophy}>🎉</div>
            <h1 className={styles.summaryTitle}>Session Complete!</h1>
            <p className={styles.summarySubtitle}>
              You’ve reinforced key concepts at optimal forgetting curve intervals.
            </p>
          </div>

          {/* Key Metrics */}
          <div className={styles.statsGrid}>
            <div className={styles.statCard}>
              <div className={styles.statValue}>{totalReviewed}</div>
              <div className={styles.statLabel}>Cards Reviewed</div>
            </div>
            <div className={styles.statCard}>
              <div className={styles.statValue}>{recallRate}%</div>
              <div className={styles.statLabel}>Recall Accuracy (≥3)</div>
            </div>
            <div className={styles.statCard}>
              <div className={styles.statValue}>{avgScore} / 5</div>
              <div className={styles.statLabel}>Average Recall Quality</div>
            </div>
          </div>

          {/* Activity Heatmap & Streak Banner */}
          <div className={styles.activityBanner}>
            <span className={styles.activityFire}>🔥</span>
            <div>
              <div className={styles.activityTitle}>Activity logged for your heatmap</div>
              <div className={styles.activitySub}>
                Your daily study streak and practice XP have been updated!
              </div>
            </div>
          </div>

          {/* Next Due Dates Table */}
          <div className={styles.dueDatesSection}>
            <h3 className={styles.dueDatesHeading}>Next Scheduled Reviews</h3>
            <div className={styles.dueDatesList}>
              {sessionResults.map((item, idx) => (
                <div key={idx} className={styles.dueDateRow}>
                  <div className={styles.dueCardInfo}>
                    <span className={styles.dueCardModule}>{item.moduleTitle}</span>
                    <span className={styles.dueCardFront}>{item.front}</span>
                  </div>
                  <div className={styles.dueCardBadge}>
                    <span
                      className={styles.qualityDot}
                      style={{ backgroundColor: RATINGS[item.quality]?.color || '#10b981' }}
                    />
                    <span>
                      {item.intervalDays === 1
                        ? 'Due tomorrow'
                        : `Due in ${item.intervalDays} days (${item.nextReviewDate})`}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Summary Actions */}
          <div className={styles.summaryFooter}>
            <Button
              variant="primary"
              size="lg"
              onClick={() => navigate('/practice')}
              className={styles.primarySummaryBtn}
            >
              Back to Practice Hub
            </Button>
            <Button
              variant="outline"
              size="lg"
              onClick={() => navigate('/dashboard')}
            >
              Go to Dashboard
            </Button>
          </div>
        </div>
      </div>
    );
  }

  const progressPercent = Math.round(((currentIndex + 1) / cards.length) * 100);

  return (
    <div className={styles.sessionOverlay}>
      {/* Top Header Bar */}
      <header className={styles.sessionTopBar}>
        <div className={styles.deckInfo}>
          <span className={styles.deckLogo}>⚡</span>
          <span className={styles.deckTitle}>
            {currentCard.roadmapTitle || 'Spaced Practice'}
          </span>
        </div>

        {/* Center Progress */}
        <div className={styles.progressContainer}>
          <div className={styles.progressText}>
            Card {currentIndex + 1} of {cards.length}
          </div>
          <div className={styles.progressBarTrack}>
            <div
              className={styles.progressBarFill}
              style={{ width: `${progressPercent}%` }}
            />
          </div>
        </div>

        {/* Exit Button */}
        <button
          type="button"
          className={styles.exitBtn}
          onClick={() => setShowExitConfirm(true)}
          title="Exit review session (Esc)"
        >
          End session
        </button>
      </header>

      {/* Main Flashcard Stage */}
      <main className={styles.stage}>
        <div
          className={`${styles.cardPerspective} ${isFlipped ? styles.isFlipped : ''}`}
          onClick={() => {
            if (!isFlipped) setIsFlipped(true);
          }}
        >
          <div className={styles.flashcard}>
            {/* FRONT FACE */}
            <div className={styles.cardFront}>
              <div className={styles.cardHeaderStrip}>
                <span className={styles.moduleBadge}>{currentCard.moduleTitle}</span>
                <span className={styles.sideBadge}>Question / Term</span>
              </div>

              <div className={styles.cardContent}>
                <h2 className={styles.frontText}>{currentCard.front}</h2>

                {/* Optional Hint Box */}
                {currentCard.hint && (
                  <div className={styles.hintWrapper} onClick={(e) => e.stopPropagation()}>
                    <button
                      type="button"
                      className={styles.hintToggleBtn}
                      onClick={() => setShowHint((prev) => !prev)}
                    >
                      <span>💡</span> {showHint ? 'Hide hint' : 'Show hint (H)'}
                    </button>

                    {showHint && (
                      <div className={styles.hintBox}>
                        <span className={styles.hintLabel}>Hint:</span>
                        <p className={styles.hintBody}>{currentCard.hint}</p>
                      </div>
                    )}
                  </div>
                )}
              </div>

              <div className={styles.cardFooterHint}>
                <span>Click card or press <strong>Space</strong> to reveal answer</span>
              </div>
            </div>

            {/* BACK FACE */}
            <div className={styles.cardBack}>
              <div className={styles.cardHeaderStrip}>
                <span className={styles.moduleBadge}>{currentCard.moduleTitle}</span>
                <span className={styles.sideBadgeAnswer}>Answer & Explanation</span>
              </div>

              <div className={styles.cardContent}>
                <div className={styles.promptReminder}>
                  <strong>Q:</strong> {currentCard.front}
                </div>

                <div className={styles.backMarkdown}>
                  {(currentCard.back || 'No explanation provided.').split('\n\n').map((para, pIdx) => {
                    if (para.startsWith('- ') || para.startsWith('* ')) {
                      return (
                        <ul key={pIdx} className={styles.backList}>
                          {para.split('\n').map((item, iIdx) => (
                            <li key={iIdx}>{item.replace(/^[-*]\s+/, '')}</li>
                          ))}
                        </ul>
                      );
                    }
                    return <p key={pIdx}>{para}</p>;
                  })}
                </div>

                {currentCard.hint && (
                  <div className={styles.analogyReminder}>
                    <span className={styles.analogyIcon}>💡</span>
                    <span>{currentCard.hint}</span>
                  </div>
                )}
              </div>

              <div className={styles.flipBackBar} onClick={(e) => e.stopPropagation()}>
                <button
                  type="button"
                  className={styles.flipBackBtn}
                  onClick={() => setIsFlipped(false)}
                >
                  ↩ Flip back
                </button>
              </div>
            </div>
          </div>
        </div>
      </main>

      {/* Rating & Actions Bottom Bar */}
      <footer className={styles.actionBar}>
        {!isFlipped ? (
          <div className={styles.unflippedActions}>
            <Button
              variant="primary"
              size="lg"
              className={styles.showAnswerBtn}
              onClick={() => setIsFlipped(true)}
            >
              Show answer <span>(Space)</span>
            </Button>
          </div>
        ) : (
          <div className={styles.ratingActions}>
            <div className={styles.ratingPrompt}>
              Rate your recall quality (SM-2 algorithm):
            </div>
            <div className={styles.ratingButtonsGrid}>
              {RATINGS.map((rate) => (
                <button
                  key={rate.quality}
                  type="button"
                  className={styles.ratingBtn}
                  style={{
                    '--rating-color': rate.color
                  }}
                  disabled={isSubmitting}
                  onClick={() => handleRate(rate.quality)}
                >
                  <span className={styles.rateKey}>{rate.key}</span>
                  <span className={styles.rateLabel}>{rate.label}</span>
                  <span className={styles.rateDesc}>{rate.desc}</span>
                </button>
              ))}
            </div>
          </div>
        )}
      </footer>

      {/* Exit Confirmation Modal */}
      {showExitConfirm && (
        <div className={styles.confirmBackdrop} onClick={() => setShowExitConfirm(false)}>
          <div className={styles.confirmModal} onClick={(e) => e.stopPropagation()}>
            <h3 className={styles.confirmTitle}>End review session?</h3>
            <p className={styles.confirmText}>
              You’ve reviewed {currentIndex} of {cards.length} cards. Your reviewed cards are
              already saved, but remaining cards will stay due for today.
            </p>
            <div className={styles.confirmActions}>
              <Button
                variant="outline"
                onClick={() => setShowExitConfirm(false)}
              >
                Continue Reviewing
              </Button>
              <Button
                variant="danger"
                onClick={() => {
                  if (sessionResults.length > 0) {
                    setIsFinished(true);
                    setShowExitConfirm(false);
                  } else {
                    navigate('/practice');
                  }
                }}
              >
                End & See Summary
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PracticeSession;
