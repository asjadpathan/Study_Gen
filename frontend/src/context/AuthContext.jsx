import { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // On mount, check localStorage for an existing session
  useEffect(() => {
    const stored = localStorage.getItem('studygen_user');
    if (stored) {
      try {
        setUser(JSON.parse(stored));
      } catch {
        localStorage.removeItem('studygen_user');
      }
    }
    setLoading(false);
  }, []);

  const login = (authResponse) => {
    // authResponse = { token, tokenType, userId, fullName, email }
    const userData = {
      token: authResponse.token,
      userId: authResponse.userId,
      fullName: authResponse.fullName,
      email: authResponse.email,
    };
    localStorage.setItem('studygen_user', JSON.stringify(userData));
    setUser(userData);
  };

  const logout = () => {
    localStorage.removeItem('studygen_user');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
