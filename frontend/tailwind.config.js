/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ['class'],
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        paper: 'var(--paper)',
        ink: 'var(--ink)',
        charcoal: 'var(--charcoal)',
        border: 'var(--border)',
        'border-strong': 'var(--border-strong)',
        accent: 'var(--accent)',
        'accent-soft': 'var(--accent-soft)',
        'accent-dim': 'var(--accent-dim)',
        surface: 'var(--surface)',
        'surface-raised': 'var(--surface-raised)',
        good: 'var(--good)',
        miss: 'var(--miss)',
      },
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'system-ui', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'monospace'],
      },
      borderRadius: {
        sm: '3px',
        lg: '14px',
        xl: '24px',
      },
    },
  },
  plugins: [],
}
