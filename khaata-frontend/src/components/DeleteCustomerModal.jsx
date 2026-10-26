import { useEffect, useState } from 'react';
import { api, rupee } from '../api';

const overlay = { position: 'fixed', inset: 0, zIndex: 30, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(4px)', padding: 16 };
const sheet = { width: '100%', maxWidth: 440, background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, padding: 24, boxShadow: '0 20px 60px rgba(0,0,0,0.5)' };
const lbl = { fontSize: 12, color: 'var(--muted)', marginBottom: 6, display: 'block', fontWeight: 600 };

export default function DeleteCustomerModal({ customer, onClose, onDeleted, onError }) {
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => { setPassword(''); setError(null); setBusy(false); }, [customer]);
  if (!customer) return null;
  const owes = Number(customer.outstanding) > 0;

  async function submit(e) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await api.deleteCustomer(customer.id, password);
      onDeleted(`${customer.name} deleted.`);
    } catch (err) { setError(err.message); onError && onError(err.message); } finally { setBusy(false); }
  }

  return (
    <div style={overlay}>
      <div style={sheet} className="animate-in">
        <h3 style={{ fontSize: 18, fontWeight: 700, marginTop: 0, marginBottom: 8, color: 'var(--overdue)' }}>Delete {customer.name}?</h3>
        <p style={{ fontSize: 14, color: 'var(--muted)', marginTop: 0, marginBottom: 16 }}>
          This permanently removes the customer and their entire credit history. This cannot be undone.
        </p>
        {owes && (
          <div className="badge badge-overdue" style={{ display: 'block', padding: '10px 12px', borderRadius: 12, marginBottom: 16, fontWeight: 500 }}>
            {customer.name} still owes <b>{rupee(customer.outstanding)}</b>. Clear the balance before deleting — the server will reject this.
          </div>
        )}
        <form onSubmit={submit} style={{ display: 'grid', gap: 12 }}>
          <div>
            <span style={lbl}>Confirm with your login password</span>
            <input type="password" autoFocus value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Your password" />
          </div>
          {error && <div className="badge badge-overdue" style={{ display: 'block', padding: '8px 12px', borderRadius: 10, fontWeight: 500 }}>{error}</div>}
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 8 }}>
            <button type="button" onClick={onClose} className="btn btn-ghost">Cancel</button>
            <button type="submit" disabled={busy || !password} className={`btn btn-outline alert ${busy ? 'is-loading' : ''}`}>Delete Customer</button>
          </div>
        </form>
      </div>
    </div>
  );
}
