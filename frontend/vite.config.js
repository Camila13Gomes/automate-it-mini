import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const backendOrigin = 'https://automate-it-mini.onrender.com'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: backendOrigin,
        changeOrigin: true,
        secure: true,
        cookieDomainRewrite: '',
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            // The browser talks to Vite on localhost, while Vite makes the
            // server-to-server request to Render. Keep the outgoing Origin
            // consistent with its rewritten Host so Spring does not treat
            // this development proxy request as a disallowed cross-origin call.
            proxyReq.setHeader('origin', backendOrigin)
          })
          proxy.on('proxyRes', (proxyRes) => {
            const cookies = proxyRes.headers['set-cookie']
            if (cookies) {
              proxyRes.headers['set-cookie'] = cookies.map(cookie =>
                cookie.replace(/;\s*Secure/gi, '')
              )
            }
          })
        },
      },
    },
  },
})
