import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const ProtectedRoute = ({ children }) => {
  const { user, loading } = useAuth();
  const location = useLocation();

  // Check if token exists in localStorage as fallback
  const token = localStorage.getItem('token') || (() => {
    try {
      const stored = localStorage.getItem('studygen_user');
      return stored ? JSON.parse(stored).token : null;
    } catch {
      return null;
    }
  })();

  if (loading) return null; // Wait for initial session loading

  if (!user && !token) {
    return <Navigate to="/auth" replace state={{ redirectTo: location.pathname }} />;
  }

  return children;
};

export default ProtectedRoute;
