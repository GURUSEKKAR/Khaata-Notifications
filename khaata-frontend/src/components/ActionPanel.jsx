import { useState } from 'react';
import { api } from '../api';

// Top-left of the layout: Add (give credit) and Clear (record repayment).
export default function ActionPanel({ customers, selectedId, onDone, onError }) {
  const [tab, setTab] = useState('add'); // 'add' | 'clear'
  const [customerId, setCustomerId] = useState(selectedId ?? '');
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);

  if (selectedId && String(selectedId) !== String(customerId)) {
    setCustomerId(selectedId);
  }

  async function submit(e) {
    e.preventDefault();
    if (!customerId) return onError('Please select a customer.');
    if (!amount || Number(amount) <= 0) return onError('Enter an amount greater than 0.');

    setBusy(true);
    try {
      await api.addTransaction(customerId, {
        type: tab === 'add' ? 'CREDIT' : 'REPAYMENT',
        amount: Number(amount),
        note: note || null,
      });
      setAmount('');
      setNote('');
      onDone(tab === 'add' ? 'Credit added.' : 'Repayment recorded.');
    } catch (err) {
      onError(err.message);
    } finally {
      setBusy(false);
    }
  }

  const isAdd = tab === 'add';
  const field =
    'w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-slate-800 placeholder-slate-400 focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/20 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100';

  return (
    <div className="animate-in rounded-2xl border border-slate-200 bg-white/80 p-5 shadow-sm backdrop-blur dark:border-slate-700 dark:bg-slate-800/70">
      <div className="mb-4 inline-flex rounded-xl bg-slate-100 p-1 dark:bg-slate-700/60">
        <button
          onClick={() => setTab('add')}
          className={`rounded-lg px-4 py-1.5 text-sm font-medium transition ${
            isAdd ? 'bg-white text-brand-600 shadow dark:bg-slate-900 dark:text-brand-300' : 'text-slate-500 dark:text-slate-300'
          }`}
        >
          ＋ Add Credit
        </button>
        <button
          onClick={() => setTab('clear')}
          className={`rounded-lg px-4 py-1.5 text-sm font-medium transition ${
            !isAdd ? 'bg-white text-emerald-600 shadow dark:bg-slate-900 dark:text-emerald-300' : 'text-slate-500 dark:text-slate-300'
          }`}
        >
          ✓ Clear / Repay
        </button>
      </div>

      <form onSubmit={submit} className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <label className="text-sm">
          <span className="mb-1 block text-slate-500 dark:text-slate-400">Customer</span>
          <select value={customerId} onChange={(e) => setCustomerId(e.target.value)} className={field}>
            <option value="">Select customer…</option>
            {customers.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name} — owes ₹{Number(c.outstanding).toFixed(2)}
              </option>
            ))}
          </select>
        </label>

        <label className="text-sm">
          <span className="mb-1 block text-slate-500 dark:text-slate-400">Amount (₹)</span>
          <input type="number" min="0" step="0.01" value={amount}
            onChange={(e) => setAmount(e.target.value)}
            placeholder={isAdd ? 'e.g. 1000' : 'e.g. 500'} className={field} />
        </label>

        <label className="text-sm sm:col-span-2">
          <span className="mb-1 block text-slate-500 dark:text-slate-400">Note (optional)</span>
          <input type="text" value={note} onChange={(e) => setNote(e.target.value)}
            placeholder={isAdd ? 'Rice, dal, oil…' : 'Cash / UPI'} className={field} />
        </label>

        <button type="submit" disabled={busy}
          className={`sm:col-span-2 rounded-xl px-4 py-2.5 font-semibold text-white shadow-sm transition disabled:opacity-60 ${
            isAdd ? 'bg-brand-500 hover:bg-brand-600' : 'bg-emerald-500 hover:bg-emerald-600'
          }`}>
          {busy ? 'Saving…' : isAdd ? 'Add Credit' : 'Record Repayment'}
        </button>
      </form>
    </div>
  );
}
