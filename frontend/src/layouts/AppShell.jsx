import React, { useState, useEffect } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import Sidebar from '../components/Sidebar';
import TopBar from '../components/TopBar';
import AIAssistantDrawer from '../components/AIAssistantDrawer';
import OfflineBanner from '../components/OfflineBanner';
import RateLimitBanner, { parseRetryAfter } from '../components/RateLimitBanner';
import { ToastProvider } from '../context/ToastContext';
import { NetworkStatusProvider } from '../context/NetworkStatusContext';
import {
  setUnauthorizedCallback,
  setRateLimitCallback,
} from '../services/axiosInstance';
import styles from './AppShell.module.css';

const AppShell = () => {
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);
  const [aiDrawerOpen, setAiDrawerOpen] = useState(false);
  const [rateLimitSeconds, setRateLimitSeconds] = useState(null);
  const navigate = useNavigate();
  const location = useLocation();

  // Register 401 unauthorized callback to trigger redirect with session-expired state
  useEffect(() => {
    setUnauthorizedCallback((redirectPath) => {
      navigate('/auth', {
        replace: true,
        state: { sessionExpired: true, redirectTo: redirectPath || location.pathname },
      });
    });
  }, [navigate, location.pathname]);

  // Register 429 rate limit callback to show non-blocking top banner with countdown
  useEffect(() => {
    setRateLimitCallback((retryAfter) => {
      const parsedSec = parseRetryAfter(retryAfter);
      setRateLimitSeconds(parsedSec);
    });
  }, []);

  const moduleMatch = location.pathname.match(/\/modules\/(\d+)/);
  const currentModuleId = moduleMatch ? parseInt(moduleMatch[1], 10) : null;

  return (
    <NetworkStatusProvider>
      <ToastProvider>
        {/* Fixed Non-blocking System Resilience Banners */}
        <OfflineBanner />
        {rateLimitSeconds > 0 && (
          <RateLimitBanner
            retryAfterSeconds={rateLimitSeconds}
            onDismiss={() => setRateLimitSeconds(null)}
          />
        )}

        <div className={styles.appLayout}>
          {/* Navigation Sidebar */}
          <Sidebar
            isOpen={mobileSidebarOpen}
            onClose={() => setMobileSidebarOpen(false)}
            onOpenAIDrawer={() => setAiDrawerOpen(true)}
          />

          {/* Main Application Area */}
          <div className={styles.mainWrapper}>
            <TopBar
              onToggleSidebar={() => setMobileSidebarOpen(!mobileSidebarOpen)}
              onOpenAIDrawer={() => setAiDrawerOpen(true)}
            />

            <main className={styles.pageContent}>
              <Outlet />
            </main>
          </div>

          {/* Global AI Assistant Drawer */}
          <AIAssistantDrawer
            isOpen={aiDrawerOpen}
            onClose={() => setAiDrawerOpen(false)}
            currentModuleId={currentModuleId}
          />
        </div>
      </ToastProvider>
    </NetworkStatusProvider>
  );
};

export default AppShell;
