import tailwindcss from '@tailwindcss/vite';
import path from 'path';
import react from '@vitejs/plugin-react';
import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    server: {
      host: '0.0.0.0',
      port: 5173,
      strictPort: true,
      proxy: {
        '/api': {
          target: env.VITE_PROXY_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            const moduleId = id.replaceAll('\\', '/');

            if (
              /node_modules\/(react|react-dom|react-router|react-router-dom)\//.test(
                moduleId,
              )
            ) {
              return 'react';
            }
            if (/node_modules\/(recharts|reactflow|dagre)\//.test(moduleId)) {
              return 'charts';
            }
            if (
              /node_modules\/(react-markdown|remark-math|remark-gfm|rehype-katex|katex)\//.test(
                moduleId,
              )
            ) {
              return 'markdown';
            }

            return undefined;
          },
        },
      },
    },
  };
});
