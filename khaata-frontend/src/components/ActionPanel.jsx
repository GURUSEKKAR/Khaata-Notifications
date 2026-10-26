import { useState } from 'react';
import { Plus, Check } from 'lucide-react';
import { api } from '../api';

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
  const fullOutstanding = selectedCustomer ? Number(selectedCustomer.outstanding) : 0;

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

  const lbl = { fontSize: 12, color: 'var(--muted)', marginBottom: 6, display: 'block', fontWeight: 600 };

  return (
    <div className="card animate-in">
      {/* Segmented tabs */}
      <div style={{ display: 'inline-flex', gap: 6, marginBottom: 18 }}>
        <button type="button" onClick={() => setTab('add')}
          className={`tab tab-credit ${isAdd ? 'active' : ''}`}>
          <Plus size={16} /> Add Credit
        </button>
        <button type="button" onClick={() => setTab('clear')}
          className={`tab tab-repay ${!isAdd ? 'active' : ''}`}>
          <Check size={16} /> Clear / Repay
        </button>
      </div>

      <form onSubmit={submit} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
        <label style={{ gridColumn: 'span 1' }}>
          <span style={lbl}>Customer</span>
          <select value={customerId} onChange={(e) => setCustomerId(e.target.value)}>
            <option value="">Select customer…</option>
            {customers.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name} — owes ₹{Number(c.outstanding).toFixed(2)}
              </option>
            ))}
          </select>
        </label>

        <label>
          <span style={lbl}>Amount (₹)</span>
          <input type="number" min="0" step="0.01" value={amount}
            onChange={(e) => setAmount(e.target.value)}
            placeholder={isAdd ? 'e.g. 1000' : 'e.g. 500'} />
        </label>

        <div style={{ gridColumn: 'span 2', display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          {QUICK_AMOUNTS.map((amt) => (
            <button key={amt} type="button"
              className={`chip ${isAdd ? 'chip-credit' : 'chip-repay'} ${String(amt) === amount ? 'active' : ''}`}
              onClick={() => setAmount(String(amt))}>
              ₹{amt}
            </button>
          ))}
          {!isAdd && fullOutstanding > 0 && (
            <button type="button" className="chip chip-repay" onClick={() => setAmount(String(fullOutstanding))}>
              Full ₹{fullOutstanding.toFixed(2)}
            </button>
          )}
        </div>

        <label style={{ gridColumn: 'span 2' }}>
          <span style={lbl}>Note (optional)</span>
          <input type="text" value={note} onChange={(e) => setNote(e.target.value)}
            placeholder={isAdd ? 'Rice, dal, oil…' : 'Cash / UPI'} />
        </label>

        <button type="submit" disabled={busy}
          className={`btn ${isAdd ? 'btn-credit' : 'btn-success'} ${busy ? 'is-loading' : ''}`}
          style={{ gridColumn: 'span 2', width: '100%' }}>
          {isAdd ? 'Add Credit' : 'Record Repayment'}
        </button>
      </form>
    </div>
  );
}
