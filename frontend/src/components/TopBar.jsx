import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import axiosInstance from '../services/axiosInstance';
import styles from './TopBar.module.css';

const pageTitleMap = {
  '/dashboard': 'Dashboard',
  '/generate-roadmap': 'Generate Roadmap',
  '/diagnostic-test': 'Diagnostic Assessment',
  '/roadmap-view': 'Roadmap View',
  '/roadmap-workspace': 'My Roadmaps Workspace',
  '/document-analyzer': 'Document & PDF Analyzer',
  '/documents': 'Documents & Library',
  '/practice': 'Practice & Flashcards',
  '/practice-hub': 'Practice & Flashcards',
  '/practice/session': 'Flashcard Review Session',
  '/chat': 'AI Chat Assistant',
  '/career': 'Career Paths',
  '/study-room': 'Study Room',
  '/resources': 'Resource Discovery',
  '/community': 'Community Zone',
  '/profile': 'User Profile',
};

const TopBar = ({ onToggleSidebar, title: customTitle }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout, refreshStreak } = useAuth();
  const { isDark, toggleTheme } = useTheme();

  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [flashcardsDue, setFlashcardsDue] = useState(0);
  const dropdownRef = useRef(null);

  const currentPath = location.pathname;
  const pageTitle = customTitle || pageTitleMap[currentPath] || 'StudyGen';

  // Fetch authentic streak & flashcards due count on mount
  useEffect(() => {
    let isMounted = true;
    if (refreshStreak) {
      refreshStreak();
    }
    axiosInstance
      .get('/practice/due')
      .then((res) => {
        if (isMounted && res.data?.dueCount !== undefined) {
          setFlashcardsDue(res.data.dueCount);
        }
      })
      .catch(() => {
        // Fallback default due count if endpoint not yet populated
        if (isMounted) setFlashcardsDue(0);
      });

    return () => {
      isMounted = false;
    };
  }, [refreshStreak]);

  // Close dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSignOut = () => {
    setDropdownOpen(false);
    logout();
    navigate('/auth');
  };

  const streakCount = typeof user?.currentStreak === 'number' ? user.currentStreak : 0;
  const longestStreak = typeof user?.longestStreak === 'number' ? user.longestStreak : streakCount;
  const userInitials = user?.fullName
    ? user.fullName
        .split(' ')
        .map((n) => n[0])
        .join('')
        .toUpperCase()
        .slice(0, 2)
    : 'SG';

  return (
    <header className={styles.topBar}>
      <div className={styles.leftSlot}>
        {/* Mobile Hamburger Button */}
        <button
          className={styles.hamburgerBtn}
          onClick={onToggleSidebar}
          aria-label="Toggle Navigation Sidebar"
        >
          ☰
        </button>

        {/* Page Title & Breadcrumb */}
        <div className={styles.breadcrumbWrapper}>
          <span className={styles.breadcrumbPrefix}>App /</span>
          <h1 className={styles.pageTitle}>{pageTitle}</h1>
        </div>
      </div>

      <div className={styles.rightSlot}>
        {/* Streak Chip */}
        <div
          className={styles.streakChip}
          title={`Current study streak: ${streakCount} day${streakCount === 1 ? '' : 's'} · Longest: ${longestStreak} day${longestStreak === 1 ? '' : 's'}`}
          onClick={() => navigate('/profile')}
          style={{ cursor: 'pointer' }}
        >
          <span className={styles.streakIcon}>🔥</span>
          <span className={styles.streakCount}>{streakCount}d</span>
        </div>

        {/* Flashcards Due Chip */}
        <div
          className={styles.flashcardsChip}
          title={`${flashcardsDue} flashcard reviews due today`}
          onClick={() => navigate('/practice')}
          style={{ cursor: 'pointer' }}
        >
          <span className={styles.flashcardsIcon}>🎴</span>
          <span className={styles.flashcardsBadge}>{flashcardsDue}</span>
        </div>

        {/* Dark / Light Mode Toggle Button */}
        <button
          type="button"
          className={styles.themeToggleBtn}
          onClick={toggleTheme}
          title={`Switch to ${isDark ? 'light' : 'dark'} mode`}
          aria-label={`Switch to ${isDark ? 'light' : 'dark'} mode`}
        >
          <span className={styles.themeIcon}>{isDark ? '☀️' : '🌙'}</span>
          <span className={styles.themeText}>{isDark ? 'Light' : 'Dark'}</span>
        </button>

        {/* User Avatar Dropdown */}
        <div className={styles.avatarWrapper} ref={dropdownRef}>
          <button
            type="button"
            className={styles.avatarBtn}
            onClick={() => setDropdownOpen(!dropdownOpen)}
            aria-label="User Account Menu"
          >
            {userInitials}
          </button>

          {dropdownOpen && (
            <div className={styles.dropdownMenu}>
              <div className={styles.userInfoHeader}>
                <p className={styles.userName}>{user?.fullName || 'StudyGen User'}</p>
                <p className={styles.userEmail}>{user?.email || 'user@example.com'}</p>
              </div>

              <div className={styles.menuDivider}></div>

              <button
                type="button"
                className={styles.menuItem}
                onClick={() => {
                  setDropdownOpen(false);
                  navigate('/profile');
                }}
              >
                👤 Profile & Settings
              </button>
              <button
                type="button"
                className={styles.menuItem}
                onClick={() => {
                  setDropdownOpen(false);
                  navigate('/roadmap-workspace');
                }}
              >
                🗺️ My Roadmaps
              </button>
              <button
                type="button"
                className={styles.menuItem}
                onClick={toggleTheme}
              >
                {isDark ? '☀️ Switch to Light Mode' : '🌙 Switch to Dark Mode'}
              </button>

              <div className={styles.menuDivider}></div>

              <button
                type="button"
                className={`${styles.menuItem} ${styles.signOutItem}`}
                onClick={handleSignOut}
              >
                🚪 Sign out
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

export default TopBar;
