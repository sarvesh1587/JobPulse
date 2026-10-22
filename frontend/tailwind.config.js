/** @type {import('tailwindcss').Config} */
const token = (name) => `rgb(var(--${name}) / <alpha-value>)`

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        paper: token('paper'),
        surface: token('surface'),
        raised: token('raised'),
        border: token('border'),
        'border-strong': token('border-strong'),
        ink: token('ink'),
        muted: token('muted'),
        accent: token('accent'),
        'accent-ink': token('accent-ink'),
        'accent-text': token('accent-text'),
        good: token('good'),
        miss: token('miss'),
        warn: token('warn'),
      },
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'system-ui', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'ui-monospace', 'monospace'],
      },
      fontSize: {
        // Mono metadata labels: small, tracked, uppercase.
        label: ['11px', { lineHeight: '16px', letterSpacing: '0.06em' }],
      },
      borderRadius: {
        DEFAULT: '8px',
        lg: '12px',
        xl: '16px',
      },
      boxShadow: {
        pop: '0 1px 0 rgb(var(--shadow) / 0.04), 0 12px 32px -12px rgb(var(--shadow) / 0.35)',
      },
      maxWidth: {
        page: '1180px',
      },
    },
  },
  plugins: [],
}
