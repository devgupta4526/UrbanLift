/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        night: {
          950: '#070708',
          900: '#0c0c0f',
          850: '#121218',
          800: '#18181f',
        },
        signal: {
          DEFAULT: '#f4b942',
          dim: '#c9a227',
          glow: '#ffe066',
        },
        aqua: {
          DEFAULT: '#2dd4bf',
          dim: '#14b8a6',
        },
      },
      fontFamily: {
        display: ['"Syne"', 'system-ui', 'sans-serif'],
        sans: ['"DM Sans"', 'system-ui', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'monospace'],
      },
      backgroundImage: {
        'grid-fade':
          'linear-gradient(to bottom, transparent 0%, rgb(7 7 8) 100%), linear-gradient(90deg, rgba(244,185,66,0.03) 1px, transparent 1px), linear-gradient(rgba(244,185,66,0.03) 1px, transparent 1px)',
        'hero-glow':
          'radial-gradient(ellipse 80% 50% at 50% -20%, rgba(244,185,66,0.15), transparent), radial-gradient(ellipse 60% 40% at 100% 0%, rgba(45,212,191,0.08), transparent)',
      },
      boxShadow: {
        card: '0 0 0 1px rgba(255,255,255,0.06), 0 24px 80px -24px rgba(0,0,0,0.8)',
        lift: '0 0 40px -10px rgba(244,185,66,0.35)',
      },
      animation: {
        'fade-up': 'fadeUp 0.6s ease-out forwards',
        shimmer: 'shimmer 2s linear infinite',
      },
      keyframes: {
        fadeUp: {
          '0%': { opacity: '0', transform: 'translateY(12px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        shimmer: {
          '0%': { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
      },
    },
  },
  plugins: [],
};
