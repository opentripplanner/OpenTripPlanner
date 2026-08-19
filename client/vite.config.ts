import { defineConfig, type Plugin } from 'vite';
import react from '@vitejs/plugin-react';
import { copyFileSync } from 'node:fs';
import { resolve } from 'node:path';

// maplibre-gl's worker script (imported with `?url` in MapView.tsx so Vite fingerprints and
// copies it) statically imports a sibling `./maplibre-gl-shared.mjs` chunk. Vite treats a `?url`
// import as an opaque asset and never follows that internal import, so the shared chunk is
// copied here under the exact unhashed filename the worker expects next to it.
function copyMaplibreGlSharedChunk(): Plugin {
  let outDir = 'output';
  let assetsDir = 'assets';
  return {
    name: 'copy-maplibre-gl-shared-chunk',
    apply: 'build',
    configResolved(config) {
      outDir = config.build.outDir;
      assetsDir = config.build.assetsDir;
    },
    writeBundle() {
      copyFileSync(
        resolve('node_modules/maplibre-gl/dist/maplibre-gl-shared.mjs'),
        resolve(outDir, assetsDir, 'maplibre-gl-shared.mjs'),
      );
    },
  };
}

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react(), copyMaplibreGlSharedChunk()],
  base: '/',
  build: {
    outDir: 'output',
    emptyOutDir: true,
  },
  optimizeDeps: {
    exclude: ['maplibre-gl'],
  },
  // @ts-ignore
  test: {
    environment: 'jsdom',
  },
});
