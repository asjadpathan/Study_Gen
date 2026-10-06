import React, { createContext, useContext, useState, useEffect } from 'react';

const NetworkStatusContext = createContext({
  isOnline: true,
  isOffline: false,
  wasOffline: false,
});

export const NetworkStatusProvider = ({ children }) => {
  const [isOnline, setIsOnline] = useState(() => navigator.onLine);
  const [wasOffline, setWasOffline] = useState(false);

  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true);
      setWasOffline(true);
      // Reset wasOffline after 3.5 seconds so "Back online" toast disappears
      const timer = setTimeout(() => {
        setWasOffline(false);
      }, 3500);
      return () => clearTimeout(timer);
    };

    const handleOffline = () => {
      setIsOnline(false);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  return (
    <NetworkStatusContext.Provider
      value={{
        isOnline,
        isOffline: !isOnline,
        wasOffline,
      }}
    >
      {children}
    </NetworkStatusContext.Provider>
  );
};

export const useNetworkStatus = () => {
  return useContext(NetworkStatusContext);
};

export default NetworkStatusContext;
