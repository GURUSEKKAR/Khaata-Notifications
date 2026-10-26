import { useEffect, useState } from 'react';
import { api, auth } from '../api';

const overlay = { position: 'fixed', inset: 0, zIndex: 20, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(4px)', padding: 16 };
const sheet = { width: '100%', maxWidth: 460, maxHeight: '90vh', overflowY: 'auto', background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, padding: 24, boxShadow: '0 20px 60px rgba(0,0,0,0.5)' };
const lbl = { fontSize: 12, color: 'var(--muted)', marginBottom: 6, display: 'block', fontWeight: 600 };

export default function ProfileModal({ open, onClose, onSaved, onError, onDeleted }) {
  const [form, setForm] = useState(null);
  const [busy, setBusy] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [delPassword, setDelPassword] = useState('');
  const [delError, setDelError] = useState(null);
  const [delBusy, setDelBusy] = useState(false);

  useEffect(() => {
    if (open) {
      setConfirmDelete(false); setDelPassword(''); setDelError(null);
      api.getProfile()
        .then((p) => setForm({ shopName: p.shopName || '', ownerName: p.ownerName || '', phone: p.phone || '', email: p.email || '', upiId: p.upiId || '' }))
        .catch((e) => onError && onError(e.message));
    }
  }, [open]);

  if (!open || !form) return null;
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  async function save(e) {
    e.preventDefault();
    setBusy(true);
    try {
      await api.updateProfile({
        shopName: form.shopName, ownerName: form.ownerName,
        phone: form.phone || '', email: form.email || null, upiId: form.upiId || '',
      });
      onSaved('Profile saved.');
    } catch (err) { onError && onError(err.message); } finally { setBusy(false); }
  }

  async function doDelete(e) {
    e.preventDefault();
    setDelError(null);
    setDelBusy(true);
    try {
      await api.deleteAccount(delPassword);
      auth.clear();
      onDeleted();
    } catch (err) { setDelError(err.message); } finally { setDelBusy(false); }
  }

  return (
    <div style={overlay}>
      <div style={sheet} className="animate-in">
        <h3 style={{ fontSize: 18, fontWeight: 700, marginTop: 0, marginBottom: 16 }}>Shop Profile</h3>

        <form onSubmit={save} style={{ display: 'grid', gap: 12 }}>
          <div><span style={lbl}>Shop name</span><input required value={form.shopName} onChange={set('shopName')} /></div>
          <div><span style={lbl}>Owner name</span><input required value={form.ownerName} onChange={set('ownerName')} /></div>
          <div><span style={lbl}>Phone</span><input value={form.phone} onChange={set('phone')} placeholder="+91…" /></div>
          <div><span style={lbl}>Email</span><input value={form.email} onChange={set('email')} placeholder="you@example.com" /></div>
          <div>
            <span style={lbl}>UPI ID <span style={{ color: 'var(--owed)' }}>— needed to send reminders</span></span>
            <input value={form.upiId} onChange={set('upiId')} placeholder="yourname@okaxis" />
          </div>
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 4 }}>
            <button type="button" onClick={onClose} className="btn btn-ghost">Close</button>
            <button type="submit" disabled={busy} className={`btn btn-primary ${busy ? 'is-loading' : ''}`}>Save</button>
          </div>
        </form>

        <hr className="divider" style={{ margin: '20px 0' }} />

        {/* Danger zone */}
        <div>
          <h4 style={{ fontSize: 14, fontWeight: 700, color: 'var(--overdue)', margin: '0 0 6px' }}>Danger zone</h4>
          <p style={{ fontSize: 12, color: 'var(--muted)', margin: '0 0 10px' }}>
            Permanently delete this shop account and ALL its customers, ledger, and reminder history. This cannot be undone.
          </p>
          {!confirmDelete ? (
            <button type="button" className="btn btn-outline alert" onClick={() => setConfirmDelete(true)}>
              Delete my account
            </button>
          ) : (
            <form onSubmit={doDelete} style={{ display: 'grid', gap: 10 }}>
              <div>
                <span style={lbl}>Confirm with your password</span>
                <input type="password" autoFocus value={delPassword} onChange={(e) => setDelPassword(e.target.value)} placeholder="Your password" />
              </div>
              {delError && <div className="badge badge-overdue" style={{ display: 'block', padding: '8px 12px', borderRadius: 10 }}>{delError}</div>}
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
                <button type="button" onClick={() => setConfirmDelete(false)} className="btn btn-ghost">Cancel</button>
                <button type="submit" disabled={delBusy || !delPassword} className={`btn btn-outline alert ${delBusy ? 'is-loading' : ''}`}>
                  Permanently Delete
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
