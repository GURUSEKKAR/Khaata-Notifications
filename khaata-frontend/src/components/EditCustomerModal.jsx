import { useEffect, useState } from 'react';
import { api } from '../api';

const field =
  'w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-slate-800 placeholder-slate-400 focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/20 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100';

export default function EditCustomerModal({ customer, onClose, onSaved, onError }) {
  const [form, setForm] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (customer) {
      setForm({
        name: customer.name || '',
        phone: customer.phone || '',
        email: customer.email || '',
        customerType: customer.customerType || 'NORMAL',
        dueWindowDays: customer.dueWindowDays ?? '',
        telegramChatId: customer.telegramChatId || '',
      });
    }
  }, [customer]);

  if (!customer || !form) return null;
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    try {
      await api.updateCustomer(customer.id, {
        name: form.name,
        phone: form.phone,
        email: form.email || null,
        customerType: form.customerType,
        dueWindowDays: form.dueWindowDays ? Number(form.dueWindowDays) : null,
        telegramChatId: form.telegramChatId || null,
      });
      onSaved('Customer updated.');
    } catch (err) {
      onError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="fixed inset-0 z-10 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-sm">
      <div className="animate-in w-full max-w-md rounded-2xl bg-white p-6 shadow-xl dark:bg-slate-800">
        <h3 className="mb-4 text-lg font-semibold text-slate-800 dark:text-slate-100">Edit {customer.name}</h3>
        <form onSubmit={submit} className="grid gap-3">
          <input required placeholder="Name" value={form.name} onChange={set('name')} className={field} />
          <input required placeholder="Phone (+91…)" value={form.phone} onChange={set('phone')} className={field} />
          <input placeholder="Email (optional)" value={form.email} onChange={set('email')} className={field} />
          <input placeholder="Telegram chat ID (optional)" value={form.telegramChatId} onChange={set('telegramChatId')} className={field} />
          <div className="grid grid-cols-2 gap-3">
            <select value={form.customerType} onChange={set('customerType')} className={field}>
              <option value="NORMAL">Normal</option>
              <option value="REGULAR">Regular</option>
            </select>
            <input type="number" min="1" placeholder="Due days" value={form.dueWindowDays} onChange={set('dueWindowDays')} className={field} />
          </div>
          <div className="mt-2 flex justify-end gap-2">
            <button type="button" onClick={onClose} className="btn btn-ghost">Cancel</button>
            <button type="submit" disabled={busy} className={`btn ${busy ? 'is-loading' : ''}`}>
              Save Changes
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
