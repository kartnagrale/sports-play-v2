/** @type {import('tailwindcss').Config} */
const token = name => `rgb(var(--${name}-rgb) / <alpha-value>)`;
module.exports = {
  darkMode: 'class',
  content: ['./src/**/*.{js,ts,jsx,tsx,mdx}'],
  safelist: ['motion-expand', 'motion-success', 'motion-error', 'motion-celebrate', 'motion-enter', 'motion-highlight'],
  theme: { extend: {
    colors: {
      white: 'rgb(var(--color-fg) / <alpha-value>)',
      bg: { DEFAULT: token('bg'), surface: token('surface'), elev: token('elev') },
      primary: { DEFAULT: token('primary'), hover: 'var(--color-primary-hover)' },
      secondary: token('secondary'), danger: token('danger'), success: token('success'), warning: token('warning'), info: token('info'), live: token('live'),
      muted: 'var(--text-muted)', foreground: 'var(--text-main)', border: 'var(--color-border)',
    },
    fontFamily: { heading: ['var(--font-outfit)','sans-serif'], body: ['var(--font-outfit)','sans-serif'], mono: ['var(--font-jetbrains)','monospace'] },
    borderRadius: { card: 'var(--radius-lg)' },
    boxShadow: { card: 'var(--shadow-card)' },
    animation: { 'pulse-slow': 'pulse 2s ease-in-out infinite', 'fade-in': 'motion-enter var(--motion-base) var(--ease-out)', 'slide-up': 'motion-enter var(--motion-base) var(--ease-out)' },
  } },
  plugins: [],
};
