import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const useProxy = env.VITE_USE_PROXY !== 'false';

  return {
    define: {
      global: "window",   // 👈 ADD THIS HERE
    },
    resolve: {
      alias: { '@': path.resolve(__dirname, 'src') },
    },
    plugins: [react()],
    server: {
      port: 5173,
      proxy: useProxy
        ? {
            '/__auth': {
              target: 'http://localhost:7475',
              changeOrigin: true,
              rewrite: (p) => p.replace(/^\/__auth/, ''),
            },
            '/__driver': {
              target: 'http://localhost:8081',
              changeOrigin: true,
              rewrite: (p) => p.replace(/^\/__driver/, ''),
            },
            '/__booking': {
              target: 'http://localhost:8001',
              changeOrigin: true,
              rewrite: (p) => p.replace(/^\/__booking/, ''),
            },
            '/__payment': {
              target: 'http://localhost:8082',
              changeOrigin: true,
              rewrite: (p) => p.replace(/^\/__payment/, ''),
            },
            '/__socket': {
              target: 'http://localhost:3002',
              changeOrigin: true,
              ws: true,
              rewrite: (p) => p.replace(/^\/__socket/, ''),
            },
          }
        : undefined,
    },
  };
});
