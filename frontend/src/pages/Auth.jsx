import React, { useState, useEffect } from 'react';
import { Link, useSearchParams, useNavigate, useLocation } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import axios from 'axios';
import { useAuth } from '../context/AuthContext';
import Button from '../components/Button';
import Card from '../components/Card';
import logoImg from '../assets/images/logo.png';
import styles from './Auth.module.css';

const API_BASE = 'http://localhost:8080/api';

const Auth = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { login: setAuthUser } = useAuth();

  // Active tab: 'login' or 'signup'
  const tabParam = searchParams.get('tab');
  const activeTab = tabParam === 'signup' ? 'signup' : 'login';

  const [showPassword, setShowPassword] = useState(false);
  const [serverError, setServerError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Check for sessionExpired flag passed from location.state or URL query params
  const sessionExpired =
    location.state?.sessionExpired ||
    new URLSearchParams(location.search).get('sessionExpired') === 'true';
  const redirectTo = location.state?.redirectTo || location.state?.from?.pathname || '/dashboard';

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors: clientErrors },
  } = useForm({
    mode: 'onBlur',
  });

  // Clear errors and reset form when switching tabs
  useEffect(() => {
    setServerError('');
    setFieldErrors({});
    reset();
  }, [activeTab, reset]);

  const handleTabSwitch = (tab) => {
    setSearchParams({ tab });
  };

  const onSubmit = async (data) => {
    setServerError('');
    setFieldErrors({});
    setIsSubmitting(true);

    try {
      const endpoint = activeTab === 'signup' 
        ? `${API_BASE}/auth/register` 
        : `${API_BASE}/auth/login`;

      const payload = activeTab === 'signup'
        ? { fullName: data.fullName, email: data.email, password: data.password }
        : { email: data.email, password: data.password };

      const response = await axios.post(endpoint, payload);
      const authData = response.data;

      // Store JWT token & set Axios default header
      if (authData?.token) {
        localStorage.setItem('token', authData.token);
        axios.defaults.headers.common['Authorization'] = `Bearer ${authData.token}`;
      }

      // Update AuthContext user state
      if (setAuthUser) {
        setAuthUser(authData);
      }

      // Navigate to destination
      navigate(redirectTo, { replace: true });
    } catch (err) {
      if (err.response) {
        const { status, data: errData } = err.response;

        if (status === 401) {
          setServerError('Email or password is incorrect.');
        } else if (status === 409) {
          setServerError('That email is already registered.');
        } else if (status === 400) {
          if (typeof errData === 'object' && !errData.error && !errData.message) {
            // Field level error map { email: "...", password: "..." }
            setFieldErrors(errData);
            setServerError('Please correct the highlighted errors below.');
          } else {
            const msg = errData.error || errData.message || 'Invalid request parameters.';
            if (msg.toLowerCase().includes('already exists') || msg.toLowerCase().includes('already registered')) {
              setServerError('That email is already registered.');
            } else {
              setServerError(msg);
            }
          }
        } else {
          setServerError(errData?.error || errData?.message || 'An unexpected error occurred. Please try again.');
        }
      } else {
        setServerError('Network error. Failed to reach server. Please check your connection.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className={styles.authContainer}>
      <Card hoverable={false} className={styles.authCard}>
        {/* Header Branding */}
        <div className={styles.cardHeader}>
          <div className={styles.brandLogoWrapper}>
            <Link to="/" aria-label="StudyGen Home">
              <img src={logoImg} alt="StudyGen" className={styles.authLogo} />
            </Link>
          </div>
          <h1 className={styles.cardTitle}>
            {activeTab === 'login' ? 'Welcome back' : 'Create your account'}
          </h1>
          <p className={styles.cardSubtitle}>
            {activeTab === 'login'
              ? 'Enter your credentials to access your roadmaps'
              : 'Start your personalized AI study path in under a minute'}
          </p>
        </div>

        {/* Tab Switcher */}
        <div className={styles.tabContainer}>
          <button
            type="button"
            className={`${styles.tab} ${activeTab === 'login' ? styles.tabActive : ''}`}
            onClick={() => handleTabSwitch('login')}
          >
            Log in
          </button>
          <button
            type="button"
            className={`${styles.tab} ${activeTab === 'signup' ? styles.tabActive : ''}`}
            onClick={() => handleTabSwitch('signup')}
          >
            Sign up
          </button>
        </div>

        {/* Session Expired Banner */}
        {sessionExpired && activeTab === 'login' && (
          <div className={styles.sessionBanner}>
            <span>⚠️</span> Your session expired. Please sign in again.
          </div>
        )}

        {/* General Form Error Banner */}
        {serverError && (
          <div className={styles.formErrorBanner}>
            <span>⚠️</span> {serverError}
          </div>
        )}

        {/* Form Body */}
        <form onSubmit={handleSubmit(onSubmit)} className={styles.authForm} noValidate>
          {/* Full Name field (Sign Up Only) */}
          {activeTab === 'signup' && (
            <div className={styles.formGroup}>
              <label htmlFor="fullName" className={styles.label}>
                Full Name
              </label>
              <input
                id="fullName"
                type="text"
                placeholder="e.g. Alex Morgan"
                className={`${styles.input} ${
                  clientErrors.fullName || fieldErrors.fullName ? styles.inputError : ''
                }`}
                {...register('fullName', {
                  required: 'Full name is required',
                  minLength: { value: 2, message: 'Name must be at least 2 characters' },
                })}
              />
              {(clientErrors.fullName || fieldErrors.fullName) && (
                <span className={styles.helperText}>
                  {clientErrors.fullName?.message || fieldErrors.fullName}
                </span>
              )}
            </div>
          )}

          {/* Email field */}
          <div className={styles.formGroup}>
            <label htmlFor="email" className={styles.label}>
              Email address
            </label>
            <input
              id="email"
              type="email"
              placeholder="you@example.com"
              className={`${styles.input} ${
                clientErrors.email || fieldErrors.email ? styles.inputError : ''
              }`}
              {...register('email', {
                required: 'Email address is required',
                pattern: {
                  value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                  message: 'Please enter a valid email address',
                },
              })}
            />
            {(clientErrors.email || fieldErrors.email) && (
              <span className={styles.helperText}>
                {clientErrors.email?.message || fieldErrors.email}
              </span>
            )}
          </div>

          {/* Password field */}
          <div className={styles.formGroup}>
            <label htmlFor="password" className={styles.label}>
              Password
            </label>
            <div className={styles.passwordWrapper}>
              <input
                id="password"
                type={showPassword ? 'text' : 'password'}
                placeholder="••••••••"
                className={`${styles.input} ${
                  clientErrors.password || fieldErrors.password ? styles.inputError : ''
                }`}
                {...register('password', {
                  required: 'Password is required',
                  minLength: {
                    value: 6,
                    message: 'Password must be at least 6 characters',
                  },
                })}
              />
              <button
                type="button"
                className={styles.eyeToggle}
                onClick={() => setShowPassword(!showPassword)}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? (
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                    <line x1="1" y1="1" x2="23" y2="23"></line>
                  </svg>
                ) : (
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                    <circle cx="12" cy="12" r="3"></circle>
                  </svg>
                )}
              </button>
            </div>
            {(clientErrors.password || fieldErrors.password) && (
              <span className={styles.helperText}>
                {clientErrors.password?.message || fieldErrors.password}
              </span>
            )}
          </div>

          {/* Submit Button */}
          <Button
            type="submit"
            variant="primary"
            size="lg"
            disabled={isSubmitting}
            className={`${styles.submitBtn} ${isSubmitting ? styles.submitting : ''}`}
          >
            {isSubmitting ? (
              <span className={styles.spinnerWrapper}>
                <span className={styles.spinner}></span>
                {activeTab === 'login' ? 'Logging in...' : 'Creating account...'}
              </span>
            ) : (
              activeTab === 'login' ? 'Log in to StudyGen' : 'Create Free Account'
            )}
          </Button>
        </form>

        {/* Footer Toggle text */}
        <div className={styles.cardFooter}>
          {activeTab === 'login' ? (
            <p>
              Don't have an account?{' '}
              <button
                type="button"
                className={styles.switchLink}
                onClick={() => handleTabSwitch('signup')}
              >
                Sign up free
              </button>
            </p>
          ) : (
            <p>
              Already have an account?{' '}
              <button
                type="button"
                className={styles.switchLink}
                onClick={() => handleTabSwitch('login')}
              >
                Log in
              </button>
            </p>
          )}
        </div>
      </Card>
    </div>
  );
};

export default Auth;
