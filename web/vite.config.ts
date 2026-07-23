import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      manifest: {
        name: 'table-order',
        short_name: 'table-order',
        start_url: '/',
        display: 'standalone',
        theme_color: '#1b5e20',
        background_color: '#ffffff',
        icons: [],
      },
    }),
  ],
})
