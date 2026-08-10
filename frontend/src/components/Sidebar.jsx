import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import './Sidebar.css'

const navItems = [
  { path: '/dashboard', label: 'Dashboard', icon: 'grid' },
  { path: '/generate-roadmap', label: 'Generate Roadmap', icon: 'compass' },
  { path: '/roadmap-view', label: 'My Roadmaps', icon: 'map' },
  { path: '/study-room', label: 'Study Room', icon: 'clock' },
  { path: '/document-analyzer', label: 'Document Analyzer', icon: 'file-text' },
  { path: '/resources', label: 'Resources', icon: 'book' },
  { path: '/community', label: 'Community', icon: 'users' },
  { path: '/profile', label: 'Profile', icon: 'user' },
]

const iconPaths = {
  grid: <><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></>,
  compass: <><circle cx="12" cy="12" r="10" /><polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76" /></>,
  map: <><polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6" /><line x1="8" y1="2" x2="8" y2="18" /><line x1="16" y1="6" x2="16" y2="22" /></>,
  clock: <><circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" /></>,
  'file-text': <><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" /><polyline points="14 2 14 8 20 8" /><line x1="16" y1="13" x2="8" y2="13" /><line x1="16" y1="17" x2="8" y2="17" /></>,
  book: <><path d="M4 19.5A2.5 2.5 0 016.5 17H20" /><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z" /></>,
  users: <><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 00-3-3.87" /><path d="M16 3.13a4 4 0 010 7.75" /></>,
  user: <><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" /><circle cx="12" cy="7" r="4" /></>,
}

const Sidebar = () => {
  const location = useLocation()
  const [collapsed, setCollapsed] = useState(false)

  return (
    <aside className={`sidebar ${collapsed ? 'sidebar--collapsed' : ''}`}>
      <div className="sidebar__header">
        <Link to="/" className="sidebar__brand">
          {collapsed ? 'E' : 'EduAI'}
        </Link>
        <button
          className="sidebar__toggle"
          onClick={() => setCollapsed((v) => !v)}
          aria-label="Toggle sidebar"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            {collapsed ? (
              <><line x1="3" y1="12" x2="21" y2="12" /><line x1="3" y1="6" x2="21" y2="6" /><line x1="3" y1="18" x2="21" y2="18" /></>
            ) : (
              <><polyline points="11 17 6 12 11 7" /><line x1="6" y1="12" x2="18" y2="12" /></>
            )}
          </svg>
        </button>
      </div>

      <nav className="sidebar__nav">
        {navItems.map((item) => (
          <Link
            key={item.path}
            to={item.path}
            className={`sidebar__link ${location.pathname === item.path ? 'sidebar__link--active' : ''}`}
          >
            <svg className="sidebar__icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              {iconPaths[item.icon]}
            </svg>
            {!collapsed && <span className="sidebar__label">{item.label}</span>}
          </Link>
        ))}
      </nav>
    </aside>
  )
}

export default Sidebar
