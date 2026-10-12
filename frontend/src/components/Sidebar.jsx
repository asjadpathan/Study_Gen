import React from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import logoImg from '../assets/images/logo.png';
import styles from './Sidebar.module.css';

const PlusIcon = () => (
  <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
    <line x1="12" y1="5" x2="12" y2="19" />
    <line x1="5" y1="12" x2="19" y2="12" />
  </svg>
);

const navGroups = [
  {
    label: 'Main',
    items: [
      { label: 'Dashboard',   path: '/dashboard',           icon: '⬡' },
      { label: 'My Roadmaps', path: '/roadmap-workspace',   icon: '🗺' },
      { label: 'Practice',    path: '/practice',            icon: '⚡' },
    ],
  },
  {
    label: 'Tools',
    items: [
      { label: 'Documents',    path: '/documents',     icon: '📄' },
      { label: 'AI Chat',      path: '/chat',          icon: '💬' },
      { label: 'Study Room',   path: '/study-room',    icon: '🎧' },
    ],
  },
  {
    label: 'Discover',
    items: [
      { label: 'Career',     path: '/career',    icon: '💼' },
      { label: 'Resources',  path: '/resources', icon: '🌐' },
      { label: 'Community',  path: '/community', icon: '👥', soon: true },
    ],
  },
  {
    label: 'Account',
    items: [
      { label: 'Profile', path: '/profile', icon: '👤' },
    ],
  },
];

const Sidebar = ({ isOpen, onClose }) => {
  const navigate = useNavigate();

  const handleNavClick = () => {
    if (onClose) onClose();
  };

  return (
    <>
      {isOpen && <div className={styles.mobileBackdrop} onClick={onClose} />}

      <aside className={`${styles.sidebar} ${isOpen ? styles.sidebarOpen : ''}`}>
        {/* Brand */}
        <Link to="/dashboard" className={styles.logoHeader} aria-label="StudyGen Dashboard">
          <img src={logoImg} alt="StudyGen" className={styles.sidebarLogo} />
        </Link>

        {/* CTA */}
        <div className={styles.actionWrapper}>
          <button
            className={styles.newRoadmapBtn}
            onClick={() => {
              if (onClose) onClose();
              navigate('/generate-roadmap');
            }}
          >
            <PlusIcon /> New Roadmap
          </button>
        </div>

        {/* Nav Groups */}
        <nav className={styles.navList}>
          {navGroups.map((group) => (
            <React.Fragment key={group.label}>
              <span className={styles.navSection}>{group.label}</span>
              {group.items.map((item, idx) => {
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
                      `${styles.navItem} ${isActive ? styles.navItemActive : ''} ${item.soon ? styles.navItemMuted : ''}`
                    }
                    onClick={(e) => handleNavClick(item, e)}
                  >
                    <span className={styles.navIcon}>{item.icon}</span>
                    <span className={styles.navLabel}>{item.label}</span>
                    {item.soon && <span className={styles.soonBadge}>Soon</span>}
                  </NavLink>
                );
              })}
            </React.Fragment>
          ))}
        </nav>
      </aside>
    </>
  );
};

export default Sidebar;
