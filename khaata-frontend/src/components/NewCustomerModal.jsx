import { useState } from 'react';
import { api } from '../api';

const overlay = { position: 'fixed', inset: 0, zIndex: 20, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(4px)', padding: 16 };
const sheet = { width: '100%', maxWidth: 440, background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, padding: 24, boxShadow: '0 20px 60px rgba(0,0,0,0.5)' };
const lbl = { fontSize: 12, color: 'var(--muted)', marginBottom: 6, display: 'block', fontWeight: 600 };

export default function NewCustomerModal({ open, onClose, onCreated, onError }) {
  const [form, setForm] = useState({ name: '', phone: '', email: '', customerType: 'NORMAL', dueWindowDays: '', telegramChatId: '' });
  const [busy, setBusy] = useState(false);
  if (!open) return null;
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    try {
      await api.createCustomer({
        name: form.name, phone: form.phone, email: form.email || null,
        customerType: form.customerType,
        dueWindowDays: form.dueWindowDays ? Number(form.dueWindowDays) : null,
        telegramChatId: form.telegramChatId || null,
      });
      setForm({ name: '', phone: '', email: '', customerType: 'NORMAL', dueWindowDays: '', telegramChatId: '' });
      onCreated('Customer added.');
    } catch (err) { onError(err.message); } finally { setBusy(false); }
  }

  return (
    <div style={overlay}>
      <div style={sheet} className="animate-in">
        <h3 style={{ fontSize: 18, fontWeight: 700, marginTop: 0, marginBottom: 16 }}>New Customer</h3>
        <form onSubmit={submit} style={{ display: 'grid', gap: 12 }}>
          <div><span style={lbl}>Name</span><input required placeholder="Name" value={form.name} onChange={set('name')} /></div>
          <div><span style={lbl}>Phone</span><input required placeholder="+91…" value={form.phone} onChange={set('phone')} /></div>
          <div><span style={lbl}>Email (optional)</span><input placeholder="email@example.com" value={form.email} onChange={set('email')} /></div>
          <div><span style={lbl}>Telegram chat ID (optional)</span><input placeholder="e.g. 8437048839" value={form.telegramChatId} onChange={set('telegramChatId')} /></div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div><span style={lbl}>Type</span>
              <select value={form.customerType} onChange={set('customerType')}>
                <option value="NORMAL">Normal (7 days)</option>
                <option value="REGULAR">Regular (30 days)</option>
              </select>
            </div>
            <div><span style={lbl}>Due days</span><input type="number" min="1" placeholder="optional" value={form.dueWindowDays} onChange={set('dueWindowDays')} /></div>
          </div>
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 8 }}>
            <button type="button" onClick={onClose} className="btn btn-ghost">Cancel</button>
            <button type="submit" disabled={busy} className={`btn btn-primary ${busy ? 'is-loading' : ''}`}>Create</button>
          </div>
        </form>
      </div>
    </div>
  );
}
