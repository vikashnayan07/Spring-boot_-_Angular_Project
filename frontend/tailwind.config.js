/** @type {import('tailwindcss').Config} */
module.exports = {
  // 👉 1. Add this content path so it reads your Angular files
  content: [
    "./src/**/*.{html,ts}",
  ],
  // 👉 2. Add this so your Dark Mode toggle button works!
  darkMode: 'class', 
  theme: {
    extend: {},
  },
  plugins: [],
}