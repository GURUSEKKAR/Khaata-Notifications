import { useEffect, useState } from 'react';

// Persists the light/dark choice in localStorage and toggles the `dark` class on
// <html>, which Tailwind's `darkMode: 'class'` uses to switch all dark: styles.
export function useTheme() {
  const [theme, setTheme] = useState(() => localStorage.getItem('khaata_theme') || 'light');

  useEffect(() => {
    const root = document.documentElement;
    if (theme === 'dark') root.classList.add('dark');
    else root.classList.remove('dark');
    localStorage.setItem('khaata_theme', theme);
  }, [theme]);

  const toggle = () => setTheme((t) => (t === 'dark' ? 'light' : 'dark'));
  return { theme, toggle };
}
