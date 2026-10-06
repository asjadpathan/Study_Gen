import axios from 'axios';

const API_BASE = 'http://localhost:8080/api';

const axiosInstance = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request Interceptor: Attach Bearer token to every request
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token') || (() => {
      try {
        const stored = localStorage.getItem('studygen_user');
        return stored ? JSON.parse(stored).token : null;
      } catch {
        return null;
      }
    })();

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptors: Handle 401 session expiration & 429 rate limiting
let onUnauthorizedCallback = null;
let onRateLimitCallback = null;

export const setUnauthorizedCallback = (callback) => {
  onUnauthorizedCallback = callback;
};

export const setRateLimitCallback = (callback) => {
  onRateLimitCallback = callback;
};

axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      if (error.response.status === 401) {
        localStorage.removeItem('token');
        localStorage.removeItem('studygen_user');
        
        if (onUnauthorizedCallback) {
          onUnauthorizedCallback(window.location.pathname);
        } else {
          window.location.href = `/auth?sessionExpired=true`;
        }
      } else if (error.response.status === 429) {
        const retryAfter =
          error.response.headers?.['retry-after'] ||
          error.response.headers?.['Retry-After'];
        if (onRateLimitCallback) {
          onRateLimitCallback(retryAfter);
        }
      }
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;
