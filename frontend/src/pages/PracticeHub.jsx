import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Card from '../components/Card';
import Button from '../components/Button';
import StatusPill from '../components/StatusPill';
import EmptyState from '../components/EmptyState';
import axiosInstance from '../services/axiosInstance';
import styles from './PracticeHub.module.css';

const PracticeHub = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [dueData, setDueData] = useState({
    dueCount: 0,
    totalCards: 0,
    streakDays: 0,
    breakdown: [],
    upcomingSchedule: [],
    dueCards: []
  });

  const fetchDueData = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await axiosInstance.get('/practice/due');
      if (res.data) {
        setDueData({
          dueCount: res.data.dueCount || 0,
          totalCards: res.data.totalCards || 0,
          streakDays: res.data.streakDays || 0,
          breakdown: res.data.breakdown || [],
          upcomingSchedule: res.data.upcomingSchedule || [],
          dueCards: res.data.dueCards || []
        });
      }
    } catch (err) {
      console.error('Failed to load practice due data:', err);
      setError('Failed to fetch your spaced repetition review schedule. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDueData();
  }, []);

  const handleStartReview = (moduleFilter = null) => {
    if (moduleFilter) {
      navigate(`/practice/session?moduleId=${moduleFilter}`);
    } else {
      navigate('/practice/session');
    }
  };

  if (loading) {
    return (
      <div className={styles.hubContainer}>
        <div className={styles.loadingSkeleton}>
          <div className={styles.skeletonHeader} />
          <div className={styles.skeletonGrid}>
            <div className={styles.skeletonCard} />
            <div className={styles.skeletonCard} />
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className={styles.hubContainer}>
        <Card className={styles.errorCard}>
          <div className={styles.errorIcon}>⚠️</div>
          <h3 className={styles.errorTitle}>Could not load practice data</h3>
          <p className={styles.errorMsg}>{error}</p>
          <Button variant="primary" onClick={fetchDueData}>
            Try Again
          </Button>
        </Card>
      </div>
    );
  }

  const { dueCount, totalCards, streakDays, breakdown, upcomingSchedule } = dueData;

  // Empty state 1: No cards yet in the system
  if (totalCards === 0) {
    return (
      <div className={styles.hubContainer}>
        <div className={styles.pageHeader}>
          <h1 className={styles.pageTitle}>Spaced Repetition Hub</h1>
          <p className={styles.pageSubtitle}>
            Optimize your long-term memory retention with automated SM-2 spaced repetition.
          </p>
        </div>

        <EmptyState
          icon="🎴"
          title="No cards yet"
          description="Your spaced repetition deck is currently empty. Generate a learning roadmap or open a module's Flashcards tab to populate concepts and start reviewing!"
          actionLabel="Explore Roadmaps"
          onAction={() => navigate('/roadmap-workspace')}
        />
      </div>
    );
  }

  return (
    <div className={styles.hubContainer}>
      {/* Header Banner */}
      <div className={styles.headerStrip}>
        <div className={styles.headerInfo}>
          <div className={styles.metaBadges}>
            <span className={styles.hubTag}>Spaced Repetition (SM-2)</span>
            {streakDays > 0 && (
              <span className={styles.streakBadge}>
                🔥 {streakDays} day{streakDays !== 1 ? 's' : ''} streak
              </span>
            )}
          </div>
          <h1 className={styles.dueTitle}>
            {dueCount > 0 ? `${dueCount} cards due today` : "You're all caught up"}
          </h1>
          <p className={styles.dueSubtitle}>
            {dueCount > 0
              ? 'Review these concepts at the optimal memory forgetting curve interval to cement mastery.'
              : 'Great work! All scheduled concept reviews for today are finished. Come back tomorrow or explore upcoming topics.'}
          </p>
        </div>

        <div className={styles.headerActions}>
          {dueCount > 0 ? (
            <Button
              variant="primary"
              size="lg"
              className={styles.startBtn}
              onClick={() => handleStartReview()}
            >
              <span>⚡</span> Start review ({dueCount})
            </Button>
          ) : (
            <Button
              variant="outline"
              size="lg"
              onClick={() => handleStartReview()}
              title="Practice all cards in review ahead mode"
            >
              <span>🎴</span> Review Ahead ({totalCards})
            </Button>
          )}
        </div>
      </div>

      {/* Main Grid: Schedule Strip & Breakdown */}
      <div className={styles.mainLayout}>
        {/* Upcoming 7-day Schedule Strip */}
        <Card className={styles.scheduleCard}>
          <div className={styles.sectionHeader}>
            <div>
              <h2 className={styles.sectionTitle}>7-Day Review Forecast</h2>
              <p className={styles.sectionSubtitle}>
                Spaced repetition distribution for the upcoming week
              </p>
            </div>
            <span className={styles.totalDeckBadge}>
              {totalCards} total card{totalCards !== 1 ? 's' : ''} in deck
            </span>
          </div>

          <div className={styles.scheduleStrip}>
            {upcomingSchedule.map((dayItem, idx) => {
              const isToday = idx === 0;
              const hasCards = dayItem.count > 0;
              return (
                <div
                  key={dayItem.date}
                  className={`${styles.scheduleCol} ${isToday ? styles.todayCol : ''} ${
                    hasCards ? styles.activeCol : ''
                  }`}
                >
                  <div className={styles.colDay}>{isToday ? 'Today' : dayItem.day}</div>
                  <div className={styles.colDate}>
                    {new Date(dayItem.date + 'T00:00:00').toLocaleDateString(undefined, {
                      month: 'short',
                      day: 'numeric'
                    })}
                  </div>
                  <div
                    className={`${styles.colCount} ${
                      dayItem.count > 0 ? styles.countBadgeHighlight : styles.countBadgeZero
                    }`}
                  >
                    {dayItem.count}
                  </div>
                  <div className={styles.colLabel}>
                    {dayItem.count === 1 ? 'card' : 'cards'}
                  </div>
                </div>
              );
            })}
          </div>
        </Card>

        {/* Due-Count Breakdown by Roadmap / Module */}
        <Card className={styles.breakdownCard}>
          <div className={styles.sectionHeader}>
            <div>
              <h2 className={styles.sectionTitle}>Due by Module</h2>
              <p className={styles.sectionSubtitle}>
                Active topics requiring immediate recall reinforcement
              </p>
            </div>
          </div>

          {dueCount === 0 ? (
            <div className={styles.allCaughtUpBox}>
              <span className={styles.caughtUpIcon}>🎉</span>
              <div>
                <h3 className={styles.caughtUpTitle}>Zero pending reviews</h3>
                <p className={styles.caughtUpText}>
                  Your retention intervals are up to date! Relax or study new modules to expand your deck.
                </p>
              </div>
            </div>
          ) : (
            <div className={styles.breakdownList}>
              {breakdown.map((item) => (
                <div
                  key={`${item.roadmapId}-${item.moduleId}`}
                  className={styles.breakdownItem}
                >
                  <div className={styles.itemInfo}>
                    <div className={styles.itemBreadcrumb}>
                      <span>{item.roadmapTitle}</span>
                    </div>
                    <h3 className={styles.itemModuleTitle}>{item.moduleTitle}</h3>
                  </div>

                  <div className={styles.itemActions}>
                    <span className={styles.dueItemBadge}>
                      {item.dueCount} card{item.dueCount !== 1 ? 's' : ''} due
                    </span>
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => handleStartReview(item.moduleId)}
                    >
                      Review Module
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
};

export default PracticeHub;
