import { useEffect, useState } from 'react';
import { api } from '../api';

const overlay = { position: 'fixed', inset: 0, zIndex: 20, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(4px)', padding: 16 };
const sheet = { width: '100%', maxWidth: 440, background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, padding: 24, boxShadow: '0 20px 60px rgba(0,0,0,0.5)' };
const lbl = { fontSize: 12, color: 'var(--muted)', marginBottom: 6, display: 'block', fontWeight: 600 };

export default function EditCustomerModal({ customer, onClose, onSaved, onError }) {
  const [form, setForm] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (customer) setForm({
      name: customer.name || '', phone: customer.phone || '', email: customer.email || '',
      customerType: customer.customerType || 'NORMAL', dueWindowDays: customer.dueWindowDays ?? '',
      telegramChatId: customer.telegramChatId || '',
    });
  }, [customer]);

  if (!customer || !form) return null;
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    try {
      await api.updateCustomer(customer.id, {
        name: form.name, phone: form.phone, email: form.email || null,
        customerType: form.customerType,
        dueWindowDays: form.dueWindowDays ? Number(form.dueWindowDays) : null,
        telegramChatId: form.telegramChatId || null,
      });
      onSaved('Customer updated.');
    } catch (err) { onError(err.message); } finally { setBusy(false); }
  }

  return (
    <div style={overlay}>
      <div style={sheet} className="animate-in">
        <h3 style={{ fontSize: 18, fontWeight: 700, marginTop: 0, marginBottom: 16 }}>Edit {customer.name}</h3>
        <form onSubmit={submit} style={{ display: 'grid', gap: 12 }}>
          <div><span style={lbl}>Name</span><input required value={form.name} onChange={set('name')} /></div>
          <div><span style={lbl}>Phone</span><input required value={form.phone} onChange={set('phone')} /></div>
          <div><span style={lbl}>Email (optional)</span><input value={form.email} onChange={set('email')} /></div>
          <div><span style={lbl}>Telegram chat ID (optional)</span><input value={form.telegramChatId} onChange={set('telegramChatId')} /></div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div><span style={lbl}>Type</span>
              <select value={form.customerType} onChange={set('customerType')}>
                <option value="NORMAL">Normal</option>
                <option value="REGULAR">Regular</option>
              </select>
            </div>
            <div><span style={lbl}>Due days</span><input type="number" min="1" value={form.dueWindowDays} onChange={set('dueWindowDays')} /></div>
          </div>
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 8 }}>
            <button type="button" onClick={onClose} className="btn btn-ghost">Cancel</button>
            <button type="submit" disabled={busy} className={`btn btn-primary ${busy ? 'is-loading' : ''}`}>Save Changes</button>
          </div>
        </form>
      </div>
    </div>
  );
}
