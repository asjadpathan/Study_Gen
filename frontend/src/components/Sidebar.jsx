import React from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import Button from './Button';
import logoImg from '../assets/images/logo.png';
import styles from './Sidebar.module.css';

const Sidebar = ({ isOpen, onClose, onOpenAIDrawer }) => {
  const navigate = useNavigate();

  const navItems = [
    { label: 'Dashboard', path: '/dashboard', icon: '📊' },
    { label: 'My Roadmaps', path: '/roadmap-workspace', icon: '🗺️' },
    { label: 'Practice', path: '/practice', icon: '🏋️' },
    { label: 'Documents', path: '/documents', icon: '📄' },
    { label: 'AI Chat', path: '/chat', icon: '💬' },
    { label: 'AI Assistant', action: 'ai-drawer', icon: '🤖' },
    { label: 'Study Room', path: '/study-room', icon: '🎧' },
    { label: 'Career', path: '/career', icon: '💼' },
    { label: 'Resources', path: '/resources', icon: '🌐' },
    { label: 'Community', path: '/community', icon: '👥', soon: true },
    { label: 'Profile', path: '/profile', icon: '👤' },
  ];

  const handleNavClick = (item, e) => {
    if (onClose) onClose();
    if (item.action === 'ai-drawer') {
      e.preventDefault();
      if (onOpenAIDrawer) onOpenAIDrawer();
    }
  };

  return (
    <>
      {/* Backdrop overlay for mobile drawer */}
      {isOpen && <div className={styles.mobileBackdrop} onClick={onClose} />}

      <aside className={`${styles.sidebar} ${isOpen ? styles.sidebarOpen : ''}`}>
        {/* Brand Logo Header */}
        <Link to="/dashboard" className={styles.logoHeader} aria-label="StudyGen Dashboard">
          <img src={logoImg} alt="StudyGen" className={styles.sidebarLogo} />
        </Link>

        {/* Pinned Action Button */}
        <div className={styles.actionWrapper}>
          <Button
            variant="primary"
            size="md"
            className={styles.newRoadmapBtn}
            onClick={() => {
              if (onClose) onClose();
              navigate('/generate-roadmap');
            }}
          >
            <span>+</span> New Roadmap
          </Button>
        </div>

        {/* Navigation Item List */}
        <nav className={styles.navList}>
          {navItems.map((item, idx) => {
            if (item.action === 'ai-drawer') {
              return (
                <button
                  key={idx}
                  type="button"
                  className={styles.navItemBtn}
                  onClick={(e) => handleNavClick(item, e)}
                >
                  <span className={styles.navIcon}>{item.icon}</span>
                  <span className={styles.navLabel}>{item.label}</span>
                </button>
              );
            }

            return (
              <NavLink
                key={idx}
                to={item.path}
                className={({ isActive }) =>
                  `${styles.navItem} ${isActive ? styles.navItemActive : ''} ${
                    item.soon ? styles.navItemMuted : ''
                  }`
                }
                onClick={(e) => handleNavClick(item, e)}
              >
                <span className={styles.navIcon}>{item.icon}</span>
                <span className={styles.navLabel}>{item.label}</span>
                {item.soon && <span className={styles.soonBadge}>Soon</span>}
              </NavLink>
            );
          })}
        </nav>
      </aside>
    </>
  );
};

export default Sidebar;
