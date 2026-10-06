import React, { useState, useEffect } from 'react';
import axiosInstance from '../services/axiosInstance';
import { useToast } from '../context/ToastContext';
import DifficultyTag from '../components/DifficultyTag';
import styles from './Resources.module.css';

const CATEGORIES = [
  'All',
  'Hackathons',
  'Open source',
  'Competitive programming',
  'Research papers',
  'Documentation',
  'Challenges',
];

export default function Resources() {
  const { addToast } = useToast();

  // Tab State: 'for-you' | 'saved'
  const [activeTab, setActiveTab] = useState('for-you');

  // Category Filter State
  const [selectedCategory, setSelectedCategory] = useState('All');

  // Resources Data State
  const [resources, setResources] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Saved resources count for tab badge
  const [savedCount, setSavedCount] = useState(0);

  // Saving in progress set
  const [savingIds, setSavingIds] = useState(new Set());

  // Fetch Resources function
  const fetchResources = async () => {
    try {
      setLoading(true);
      setError(null);

      if (activeTab === 'for-you') {
        const url =
          selectedCategory === 'All'
            ? '/resources'
            : `/resources?category=${encodeURIComponent(selectedCategory)}`;
        const res = await axiosInstance.get(url);
        setResources(res.data || []);
      } else {
        const res = await axiosInstance.get('/resources/saved');
        setResources(res.data || []);
        setSavedCount((res.data || []).length);
      }
    } catch (err) {
      console.error('Failed to load resources:', err);
      setError('Could not load educational resources. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  // Preload saved count on mount
  useEffect(() => {
    const fetchSavedCount = async () => {
      try {
        const res = await axiosInstance.get('/resources/saved');
        setSavedCount((res.data || []).length);
      } catch (err) {
        console.warn('Could not fetch saved count:', err);
      }
    };
    fetchSavedCount();
  }, []);

  useEffect(() => {
    fetchResources();
  }, [activeTab, selectedCategory]);

  // Handle Save / Unsave Bookmark Toggle
  const handleToggleSave = async (resource) => {
    const resourceId = resource.id;
    if (savingIds.has(resourceId)) return;

    setSavingIds((prev) => new Set(prev).add(resourceId));
    const wasSaved = resource.saved;

    // Optimistic UI update
    setResources((prev) =>
      prev.map((r) => (r.id === resourceId ? { ...r, saved: !wasSaved } : r))
    );

    try {
      if (wasSaved) {
        await axiosInstance.delete(`/resources/${resourceId}/save`);
        addToast('Removed from saved library.', 'info');
        setSavedCount((prev) => Math.max(0, prev - 1));

        // If currently in saved tab, filter out
        if (activeTab === 'saved') {
          setResources((prev) => prev.filter((r) => r.id !== resourceId));
        }
      } else {
        await axiosInstance.post(`/resources/${resourceId}/save`);
        addToast('Saved to your library! 🔖', 'success');
        setSavedCount((prev) => prev + 1);
      }
    } catch (err) {
      console.error('Failed to update bookmark status:', err);
      // Revert optimistic update
      setResources((prev) =>
        prev.map((r) => (r.id === resourceId ? { ...r, saved: wasSaved } : r))
      );
      addToast('Could not update saved status. Please retry.', 'error');
    } finally {
      setSavingIds((prev) => {
        const next = new Set(prev);
        next.delete(resourceId);
        return next;
      });
    }
  };

  return (
    <div className={styles.container}>
      {/* Header Section */}
      <header className={styles.header}>
        <div className={styles.headerText}>
          <div className={styles.badgeRow}>
            <span className={styles.ecosystemBadge}>🌐 Developer Ecosystem</span>
          </div>
          <h1 className={styles.title}>Ecosystem & Technical Resources</h1>
          <p className={styles.subtitle}>
            Explore curated hackathons, open-source repositories, competitive programming platforms,
            seminal research papers, and technical challenges tailored to your study roadmaps.
          </p>
        </div>

        {/* Tab Switcher */}
        <div className={styles.tabButtons}>
          <button
            className={`${styles.tabBtn} ${activeTab === 'for-you' ? styles.tabBtnActive : ''}`}
            onClick={() => setActiveTab('for-you')}
          >
            <span>🎯 For you</span>
          </button>
          <button
            className={`${styles.tabBtn} ${activeTab === 'saved' ? styles.tabBtnActive : ''}`}
            onClick={() => setActiveTab('saved')}
          >
            <span>🔖 Saved</span>
            {savedCount > 0 && <span className={styles.counterBadge}>{savedCount}</span>}
          </button>
        </div>
      </header>

      {/* Persistent Notice Banner about verifying AI-suggested resources */}
      <div className={styles.noticeBanner}>
        <div className={styles.noticeIcon}>🛡️</div>
        <div className={styles.noticeContent}>
          <h4 className={styles.noticeTitle}>Verifying AI-Suggested Resources & External Contests</h4>
          <p className={styles.noticeText}>
            Resources labeled <strong>AI suggested</strong> are dynamically analyzed and aligned with your
            active engineering roadmaps. Always check official contest schedules, eligibility guidelines,
            and repository contributor docs before submitting entries or papers.
          </p>
        </div>
      </div>

      {/* Category Filter Pill Row (Shown only for "For you" tab) */}
      {activeTab === 'for-you' && (
        <div className={styles.filterSection}>
          <div className={styles.filterPillsRow}>
            {CATEGORIES.map((cat) => {
              const isActive = selectedCategory === cat;
              return (
                <button
                  key={cat}
                  className={`${styles.pillBtn} ${isActive ? styles.pillBtnActive : ''}`}
                  onClick={() => setSelectedCategory(cat)}
                >
                  {cat}
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* Content Feed Section */}
      <main className={styles.contentFeed}>
        {/* Loading Spinner */}
        {loading && (
          <div className={styles.loadingState}>
            <div className={styles.spinner}></div>
            <span>Loading curated resources...</span>
          </div>
        )}

        {/* General Error State */}
        {!loading && error && (
          <div className={styles.errorState}>
            <span className={styles.errorIcon}>⚠️</span>
            <h3 className={styles.errorTitle}>Failed to Load Resources</h3>
            <p className={styles.errorMsg}>{error}</p>
            <button onClick={fetchResources} className={styles.retryBtn}>
              Try Again
            </button>
          </div>
        )}

        {/* Empty States */}
        {!loading && !error && resources.length === 0 && (
          <div className={styles.emptyState}>
            {activeTab === 'for-you' ? (
              <>
                <span className={styles.emptyIcon}>🔍</span>
                <h3 className={styles.emptyTitle}>No Resources Found</h3>
                <p className={styles.emptyDesc}>
                  There are currently no active resources listed under <strong>{selectedCategory}</strong>.
                </p>
                <button onClick={() => setSelectedCategory('All')} className={styles.emptyActionBtn}>
                  Show All Categories
                </button>
              </>
            ) : (
              <>
                <span className={styles.emptyIcon}>📂</span>
                <h3 className={styles.emptyTitle}>No Saved Resources Yet</h3>
                <p className={styles.emptyDesc}>
                  Bookmark interesting hackathons, papers, open-source projects, or docs to build your
                  personal reference library.
                </p>
                <button onClick={() => setActiveTab('for-you')} className={styles.emptyActionBtn}>
                  Explore "For You" Feed →
                </button>
              </>
            )}
          </div>
        )}

        {/* Resources Cards Grid */}
        {!loading && !error && resources.length > 0 && (
          <div className={styles.cardsGrid}>
            {resources.map((item) => {
              const isAi = item.sourceLabel === 'AI suggested';
              const isSaved = item.saved;
              const isSaving = savingIds.has(item.id);

              return (
                <article key={item.id} className={styles.resourceCard}>
                  {/* Card Top Badges & Bookmark */}
                  <div className={styles.cardHeader}>
                    <div className={styles.badgesCluster}>
                      <span className={styles.categoryTag}>{item.category}</span>
                      <DifficultyTag level={item.difficulty} />
                      <span
                        className={`${styles.sourcePill} ${
                          isAi ? styles.sourceAi : styles.sourceCurated
                        }`}
                      >
                        {isAi ? '✨ AI suggested' : '💎 Curated'}
                      </span>
                      {item.confidence && (
                        <span className={styles.confidenceTag}>{item.confidence}</span>
                      )}
                    </div>

                    <button
                      className={`${styles.bookmarkBtn} ${isSaved ? styles.bookmarkActive : ''}`}
                      onClick={() => handleToggleSave(item)}
                      disabled={isSaving}
                      title={isSaved ? 'Remove from saved' : 'Save to library'}
                      aria-label={isSaved ? 'Unsave resource' : 'Save resource'}
                    >
                      <svg
                        width="18"
                        height="18"
                        viewBox="0 0 24 24"
                        fill={isSaved ? 'currentColor' : 'none'}
                        stroke="currentColor"
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      >
                        <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path>
                      </svg>
                    </button>
                  </div>

                  {/* Title & Summary */}
                  <h3 className={styles.cardTitle}>{item.title}</h3>
                  <p className={styles.cardDescription}>{item.description}</p>

                  {/* Why this fits you box */}
                  {item.whyFitsYou && (
                    <div className={styles.whyFitsBox}>
                      <span className={styles.whyIcon}>🎯</span>
                      <p className={styles.whyText}>
                        <strong>Why this fits you:</strong> {item.whyFitsYou}
                      </p>
                    </div>
                  )}

                  {/* Card Actions Footer */}
                  <div className={styles.cardFooter}>
                    {item.url && item.url.startsWith('http') ? (
                      <a
                        href={item.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className={styles.openBtn}
                      >
                        <span>Open Resource</span>
                        <span className={styles.arrowIcon}>↗</span>
                      </a>
                    ) : (
                      <a
                        href={`https://www.google.com/search?q=${encodeURIComponent(
                          item.searchQuery || item.title
                        )}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className={styles.searchBtn}
                      >
                        <span>Search: "{item.searchQuery || item.title}"</span>
                        <span className={styles.arrowIcon}>🔍</span>
                      </a>
                    )}
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </main>
    </div>
  );
}
