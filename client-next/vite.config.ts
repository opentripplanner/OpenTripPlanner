import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  base: '/debug-client-preview/',
  build: {
    outDir: 'output',
    emptyOutDir: true,
  },
  // @ts-ignore
  test: {
    environment: 'jsdom',
  },
});
