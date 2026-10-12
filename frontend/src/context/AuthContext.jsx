import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import axiosInstance from '../services/axiosInstance';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Update streak directly in state, localStorage, and notify all listeners
  const updateStreak = useCallback((currentStreak, longestStreak) => {
    setUser((prev) => {
      if (!prev) return prev;
      const validCurrent = typeof currentStreak === 'number' ? currentStreak : (prev.currentStreak ?? 0);
      const validLongest = typeof longestStreak === 'number'
        ? longestStreak
        : Math.max(validCurrent, prev.longestStreak ?? validCurrent);

      const updated = {
        ...prev,
        currentStreak: validCurrent,
        longestStreak: validLongest,
      };

      try {
        localStorage.setItem('studygen_user', JSON.stringify(updated));
      } catch (e) {
        console.error('Failed to save user to localStorage', e);
      }
      return updated;
    });
  }, []);

  // Fetch latest authentic streak from backend database
  const refreshStreak = useCallback(async () => {
    const token = localStorage.getItem('token');
    if (!token) return;

    try {
      const res = await axiosInstance.get('/activity/streak');
      if (res.data) {
        const cur = res.data.currentStreak ?? 0;
        const long = res.data.longestStreak ?? cur;
        updateStreak(cur, long);
        window.dispatchEvent(
          new CustomEvent('studygen_streak_updated', {
            detail: { currentStreak: cur, longestStreak: long },
          })
        );
        return { currentStreak: cur, longestStreak: long };
      }
    } catch {
      // In case /activity/streak is unavailable, fallback to /users/me/profile
      try {
        const profRes = await axiosInstance.get('/users/me/profile');
        if (profRes.data && profRes.data.currentStreak !== undefined) {
          const cur = profRes.data.currentStreak;
          updateStreak(cur, cur);
          window.dispatchEvent(
            new CustomEvent('studygen_streak_updated', {
              detail: { currentStreak: cur, longestStreak: cur },
            })
          );
          return { currentStreak: cur, longestStreak: cur };
        }
      } catch {
        // Silently catch if unauthenticated or offline
      }
    }
  }, [updateStreak]);

  // On mount, check localStorage for an existing session
  useEffect(() => {
    const stored = localStorage.getItem('studygen_user');
    if (stored) {
      try {
        const parsed = JSON.parse(stored);
        setUser(parsed);
      } catch {
        localStorage.removeItem('studygen_user');
      }
    }
    setLoading(false);
  }, []);

  // When user is restored/logged in, fetch authentic streak from backend
  useEffect(() => {
    if (user?.token) {
      refreshStreak();
    }
  }, [user?.token, refreshStreak]);

  // Listen to custom streak update event from any page/component and cross-tab storage
  useEffect(() => {
    const handleCustomStreakUpdate = (e) => {
      if (e.detail) {
        const { currentStreak, longestStreak } = e.detail;
        updateStreak(currentStreak, longestStreak);
      }
    };

    const handleStorageChange = (e) => {
      if (e.key === 'studygen_user' && e.newValue) {
        try {
          const updated = JSON.parse(e.newValue);
          setUser(updated);
        } catch {
          // Ignore parse errors
        }
      }
    };

    window.addEventListener('studygen_streak_updated', handleCustomStreakUpdate);
    window.addEventListener('storage', handleStorageChange);

    return () => {
      window.removeEventListener('studygen_streak_updated', handleCustomStreakUpdate);
      window.removeEventListener('storage', handleStorageChange);
    };
  }, [updateStreak]);

  const login = (authResponse) => {
    // authResponse = { token, tokenType, userId, fullName, email, currentStreak }
    const initialStreak = authResponse.currentStreak ?? 0;
    const userData = {
      token: authResponse.token,
      userId: authResponse.userId,
      fullName: authResponse.fullName,
      email: authResponse.email,
      currentStreak: initialStreak,
      longestStreak: initialStreak,
    };
    localStorage.setItem('studygen_user', JSON.stringify(userData));
    localStorage.setItem('token', authResponse.token);
    setUser(userData);

    setTimeout(() => {
      refreshStreak();
    }, 100);
  };

  const logout = () => {
    localStorage.removeItem('studygen_user');
    localStorage.removeItem('token');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, updateStreak, refreshStreak }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
