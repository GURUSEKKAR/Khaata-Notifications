import { useState } from 'react';
import { api, auth } from '../api';

const lbl = { fontSize: 12, color: 'var(--muted)', marginBottom: 6, display: 'block', fontWeight: 600 };

export default function Login({ onLoggedIn }) {
  const [mode, setMode] = useState('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const isSignup = mode === 'signup';

  function switchMode() { setMode(isSignup ? 'login' : 'signup'); setError(null); }

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
    } catch (err) { setError(err.message); } finally { setBusy(false); }
  }

  return (
    <div className="app-bg" style={{ display: 'flex', height: '100%', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
      <div className="card animate-in" style={{ width: '100%', maxWidth: 380, padding: 32 }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <div style={{ margin: '0 auto 12px', width: 56, height: 56, display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: 16, background: 'var(--raised)', border: '1px solid var(--border)', fontSize: 26 }}>📒</div>
          <h1 style={{ fontSize: 28, fontWeight: 700, margin: 0 }}>Khaata</h1>
          <p style={{ fontSize: 13, color: 'var(--muted)', marginTop: 4 }}>
            {isSignup ? 'Create your shop account' : 'Shop owner login'}
          </p>
        </div>

        <form onSubmit={submit} style={{ display: 'grid', gap: 16 }}>
          <div><span style={lbl}>Username</span><input autoFocus value={username} onChange={(e) => setUsername(e.target.value)} placeholder="owner" /></div>
          {isSignup && (
            <div><span style={lbl}>Shop / Display name</span><input value={displayName} onChange={(e) => setDisplayName(e.target.value)} placeholder="My Kirana Store" /></div>
          )}
          <div><span style={lbl}>Password</span><input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder={isSignup ? 'at least 6 characters' : '••••••••'} /></div>

          {error && <div className="badge badge-overdue" style={{ display: 'block', padding: '8px 12px', borderRadius: 10, fontWeight: 500 }}>{error}</div>}

          <button type="submit" disabled={busy} className={`btn btn-primary ${busy ? 'is-loading' : ''}`} style={{ width: '100%', marginTop: 4 }}>
            {isSignup ? 'Create Account' : 'Sign In'}
          </button>
        </form>

        <div style={{ textAlign: 'center', fontSize: 13, color: 'var(--muted)', marginTop: 20 }}>
          {isSignup ? 'Already have an account?' : "Don't have an account?"}
          <button onClick={switchMode} className="btn btn-ghost" style={{ minHeight: 'auto', padding: '0 6px', display: 'inline' }}>
            {isSignup ? 'Sign in' : 'Create one'}
          </button>
        </div>
        {!isSignup && (
          <p style={{ textAlign: 'center', fontSize: 11, color: 'var(--disabled)', marginTop: 16 }}>
            Default dev login: <span style={{ fontFamily: 'monospace' }}>owner / khaata123</span>
          </p>
        )}
      </div>
    </div>
  );
}
