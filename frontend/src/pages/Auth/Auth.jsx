import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import AuthSocials from './components/AuthSocials'
import LoginForm from './components/LoginForm'
import SignUpForm from './components/SignUpForm'
import './Auth.css'

const AuthPage = () => {
  const [activeTab, setActiveTab] = useState('signin')
  const { user, loading } = useAuth()

  if (loading) return null
  if (user) return <Navigate to="/dashboard" replace />

  return (
    <main className="auth">
      <div className="auth__container">
        {/* -------- Left Panel -------- */}
        <div className="auth__left">
          <div className="auth__left-content">
            {/* Placeholder image block */}
            <div className="auth__image-block">
              <svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect width="48" height="48" rx="10" fill="rgba(255,255,255,0.15)" />
                <rect x="12" y="12" width="24" height="24" rx="4" stroke="rgba(255,255,255,0.5)" strokeWidth="2" fill="none" />
                <circle cx="19" cy="20" r="3" fill="rgba(255,255,255,0.5)" />
                <path d="M12 32l8-8 6 6 4-4 6 6" stroke="rgba(255,255,255,0.5)" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </div>

            <span className="auth__badge">AI PERSONALIZED</span>
            <h2 className="auth__quote">
              &ldquo;Your journey to mastery starts with a single click.&rdquo;
            </h2>
            <div className="auth__tagline">
              <span className="auth__tagline-line" />
              <span className="auth__tagline-text">Unlock your potential with EduAI</span>
            </div>
          </div>
        </div>

        {/* -------- Right Panel -------- */}
        <div className="auth__right">
          <div className="auth__form-wrapper">
            <h1 className="auth__title">Welcome Back</h1>
            <p className="auth__subtitle">Access your personalized learning path today.</p>

            {/* Tab Switcher */}
            <div className="auth__tabs">
              <button
                className={`auth__tab ${activeTab === 'signin' ? 'auth__tab--active' : ''}`}
                onClick={() => setActiveTab('signin')}
              >
                Sign In
              </button>
              <button
                className={`auth__tab ${activeTab === 'signup' ? 'auth__tab--active' : ''}`}
                onClick={() => setActiveTab('signup')}
              >
                Create Account
              </button>
            </div>

            {/* Social Auth (Google + Divider) */}
            <AuthSocials />

            {/* Form — switches between Login and Signup */}
            {activeTab === 'signin' ? (
              <LoginForm onSwitchTab={() => setActiveTab('signup')} />
            ) : (
              <SignUpForm onSwitchTab={() => setActiveTab('signin')} />
            )}

            {/* AI Quick Tip */}
            <div className="auth__tip">
              <div className="auth__tip-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                </svg>
              </div>
              <div className="auth__tip-content">
                <span className="auth__tip-title">AI Quick Tip</span>
                <span className="auth__tip-text">
                  Users who set a daily 15-minute goal are 3x more likely to master a new skill this month.
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  )
}

export default AuthPage
