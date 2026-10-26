import { useState } from 'react';
import { api, auth } from '../api';

const field =
  'w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-slate-800 placeholder-slate-400 focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/20 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100';

export default function Login({ onLoggedIn, theme, onToggleTheme }) {
  const [mode, setMode] = useState('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const isSignup = mode === 'signup';

  function switchMode() {
    setMode(isSignup ? 'login' : 'signup');
    setError(null);
  }

  async function submit(e) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const res = isSignup
        ? await api.register({ username, password, displayName })
        : await api.login({ username, password });
      auth.setToken(res.token);
      onLoggedIn(res);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="app-bg flex h-full items-center justify-center p-4">
      <button onClick={onToggleTheme} aria-label="Toggle theme" className="btn btn-ghost btn-icon absolute right-4 top-4">
        {theme === 'dark' ? '☀️' : '🌙'}
      </button>

      <div className="animate-in w-full max-w-sm rounded-2xl border border-slate-200 bg-white/90 p-8 shadow-xl backdrop-blur dark:border-slate-700 dark:bg-slate-800/90">
        <div className="mb-6 text-center">
          <div className="mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-2xl bg-brand-500 text-2xl shadow-lg">📒</div>
          <h1 className="text-3xl font-bold text-brand-600 dark:text-brand-300">Khaata</h1>
          <p className="mt-1 text-sm text-slate-400">
            {isSignup ? 'Create your shop account' : 'Shop owner login'}
          </p>
        </div>

        <form onSubmit={submit} className="grid gap-4">
          <label className="text-sm">
            <span className="mb-1 block text-slate-500 dark:text-slate-400">Username</span>
            <input autoFocus value={username} onChange={(e) => setUsername(e.target.value)} className={field} placeholder="owner" />
          </label>

          {isSignup && (
            <label className="text-sm">
              <span className="mb-1 block text-slate-500 dark:text-slate-400">Shop / Display name</span>
              <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} className={field} placeholder="My Kirana Store" />
            </label>
          )}

          <label className="text-sm">
            <span className="mb-1 block text-slate-500 dark:text-slate-400">Password</span>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} className={field}
              placeholder={isSignup ? 'at least 6 characters' : '••••••••'} />
          </label>

          {error && <div className="rounded-xl bg-rose-50 px-3 py-2 text-sm text-rose-600 dark:bg-rose-900/30 dark:text-rose-300">{error}</div>}

          <button type="submit" disabled={busy}
            className={`btn mt-2 w-full ${busy ? 'is-loading' : ''}`}>
            {isSignup ? 'Create Account' : 'Sign In'}
          </button>
        </form>

        <div className="mt-5 text-center text-sm text-slate-500 dark:text-slate-400">
          {isSignup ? 'Already have an account?' : "Don't have an account?"}
          <button onClick={switchMode} className="ml-1 font-medium text-brand-600 hover:underline dark:text-brand-300">
            {isSignup ? 'Sign in' : 'Create one'}
          </button>
        </div>

        {!isSignup && (
          <p className="mt-4 text-center text-xs text-slate-400">
            Default dev login: <span className="font-mono">owner / khaata123</span>
          </p>
        )}
      </div>
    </div>
  );
}
