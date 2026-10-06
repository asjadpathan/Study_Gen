import React from 'react';
import { useNetworkStatus } from '../context/NetworkStatusContext';
import styles from './Button.module.css';

const Button = ({
  children,
  variant = 'primary', // 'primary' | 'outlined' | 'ghost' | 'inverted'
  size = 'md', // 'sm' | 'md' | 'lg'
  onClick,
  className = '',
  type = 'button',
  disabled = false,
  title,
  ...props
}) => {
  let isOffline = false;
  try {
    const netStatus = useNetworkStatus();
    isOffline = !!netStatus?.isOffline;
  } catch {
    // If rendered outside NetworkStatusProvider, fallback to navigator.onLine
    isOffline = typeof navigator !== 'undefined' && !navigator.onLine;
  }

  const isSubmitOfflineDisabled = type === 'submit' && isOffline;
  const effectivelyDisabled = disabled || isSubmitOfflineDisabled;
  const effectiveTitle = isSubmitOfflineDisabled
    ? 'Cannot submit forms while offline'
    : title;

  const variantClass = styles[variant] || styles.primary;
  const sizeClass = styles[size] || styles.md;

  return (
    <button
      type={type}
      className={`${styles.button} ${variantClass} ${sizeClass} ${className}`}
      onClick={onClick}
      disabled={effectivelyDisabled}
      title={effectiveTitle}
      {...props}
    >
      {children}
    </button>
  );
};

export default Button;
