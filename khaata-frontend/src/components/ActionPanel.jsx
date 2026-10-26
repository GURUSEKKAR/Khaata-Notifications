import { useState } from 'react';
import { api } from '../api';

// Top-left of the layout: Add (give credit) and Clear (record repayment).
// The submit button is amber on the Credit tab and emerald on the Repay tab, so
// the shopkeeper can't accidentally record the wrong kind of entry.
const QUICK_AMOUNTS = [100, 200, 500, 1000];

export default function ActionPanel({ customers, selectedId, onDone, onError }) {
  const [tab, setTab] = useState('add'); // 'add' | 'clear'
  const [customerId, setCustomerId] = useState(selectedId ?? '');
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);

  if (selectedId && String(selectedId) !== String(customerId)) {
    setCustomerId(selectedId);
  }

  const selectedCustomer = customers.find((c) => String(c.id) === String(customerId));
  const isAdd = tab === 'add';

  async function submit(e) {
    e.preventDefault();
    if (!customerId) return onError('Please select a customer.');
    if (!amount || Number(amount) <= 0) return onError('Enter an amount greater than 0.');

    setBusy(true);
    try {
      await api.addTransaction(customerId, {
        type: isAdd ? 'CREDIT' : 'REPAYMENT',
        amount: Number(amount),
        note: note || null,
      });
      setAmount('');
      setNote('');
      onDone(isAdd ? 'Credit added.' : 'Repayment recorded.');
    } catch (err) {
      onError(err.message);
    } finally {
      setBusy(false);
    }
  }

  const field =
    'w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-slate-800 placeholder-slate-400 focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/20 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100';

  // On the Repay tab, offer a "Full ₹X" chip for the customer's outstanding.
  const fullOutstanding = selectedCustomer ? Number(selectedCustomer.outstanding) : 0;

  return (
    <div className="animate-in rounded-2xl border border-slate-200 bg-white/80 p-5 shadow-sm backdrop-blur dark:border-slate-700 dark:bg-slate-800/70">
      {/* Segmented toggle */}
      <div className="mb-4 inline-flex rounded-xl bg-slate-100 p-1 dark:bg-slate-700/60">
        <button
          type="button"
          onClick={() => setTab('add')}
          className={`flex items-center gap-1.5 rounded-lg px-4 py-1.5 text-sm font-semibold transition ${
            isAdd ? 'bg-amber-100 text-amber-700 shadow dark:bg-amber-500/20 dark:text-amber-300' : 'text-slate-500 dark:text-slate-300'
          }`}
        >
          ＋ Add Credit
        </button>
        <button
          type="button"
          onClick={() => setTab('clear')}
          className={`flex items-center gap-1.5 rounded-lg px-4 py-1.5 text-sm font-semibold transition ${
            !isAdd ? 'bg-emerald-100 text-emerald-700 shadow dark:bg-emerald-500/20 dark:text-emerald-300' : 'text-slate-500 dark:text-slate-300'
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

        {/* Quick-amount chips */}
        <div className="flex flex-wrap gap-2 sm:col-span-2">
          {QUICK_AMOUNTS.map((amt) => (
            <button key={amt} type="button" className="chip" onClick={() => setAmount(String(amt))}>
              ₹{amt}
            </button>
          ))}
          {!isAdd && fullOutstanding > 0 && (
            <button type="button" className="chip" onClick={() => setAmount(String(fullOutstanding))}>
              Full ₹{fullOutstanding.toFixed(2)}
            </button>
          )}
        </div>

        <label className="text-sm sm:col-span-2">
          <span className="mb-1 block text-slate-500 dark:text-slate-400">Note (optional)</span>
          <input type="text" value={note} onChange={(e) => setNote(e.target.value)}
            placeholder={isAdd ? 'Rice, dal, oil…' : 'Cash / UPI'} className={field} />
        </label>

        <button type="submit" disabled={busy}
          className={`btn sm:col-span-2 w-full ${isAdd ? 'btn-credit' : 'btn-success'} ${busy ? 'is-loading' : ''}`}>
          {isAdd ? 'Add Credit' : 'Record Repayment'}
        </button>
      </form>
    </div>
  );
}
