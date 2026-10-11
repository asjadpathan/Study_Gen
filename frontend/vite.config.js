import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // Load environment variables from the common project root
  const rootDir = path.resolve(__dirname, '..');
  const env = loadEnv(mode, rootDir, '');

  const backendTarget = env.VITE_BACKEND_TARGET || 'http://localhost:8080';
  const port = parseInt(env.VITE_PORT || '5173', 10);

  return {
    plugins: [react()],
    envDir: rootDir,
    server: {
      port,
      proxy: {
        '/api': {
          target: backendTarget,
          changeOrigin: true,
        },
      },
    },
  };
});
