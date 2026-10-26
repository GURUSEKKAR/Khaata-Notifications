import { useEffect, useState } from 'react';
import { api, rupee } from '../api';

// Confirmation modal to delete a customer. Requires re-entering the owner's own
// login password. The backend also blocks deletion if the customer still owes money.
export default function DeleteCustomerModal({ customer, onClose, onDeleted, onError }) {
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  // Reset whenever the target customer changes, so a password typed for one
  // customer never carries over to another.
  useEffect(() => {
    setPassword('');
    setError(null);
    setBusy(false);
  }, [customer]);

  if (!customer) return null;
  const owes = Number(customer.outstanding) > 0;

  async function submit(e) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await api.deleteCustomer(customer.id, password);
      onDeleted(`${customer.name} deleted.`);
    } catch (err) {
      setError(err.message);
      onError && onError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="fixed inset-0 z-20 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-sm">
      <div className="animate-in w-full max-w-md rounded-2xl bg-white p-6 shadow-xl dark:bg-slate-800">
        <h3 className="mb-2 text-lg font-semibold text-rose-600 dark:text-rose-400">Delete {customer.name}?</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          This permanently removes the customer and their entire credit history. This cannot be undone.
        </p>

        {owes && (
          <div className="mb-4 rounded-xl bg-amber-50 px-3 py-2 text-sm text-amber-700 dark:bg-amber-900/30 dark:text-amber-300">
            {customer.name} still owes <b>{rupee(customer.outstanding)}</b>. Clear the balance
            before deleting — the server will reject this delete.
          </div>
        )}

        <form onSubmit={submit} className="grid gap-3">
          <label className="text-sm">
            <span className="mb-1 block text-slate-500 dark:text-slate-400">Confirm with your login password</span>
            <input type="password" autoFocus value={password} onChange={(e) => setPassword(e.target.value)}
              className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-slate-800 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400/20 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
              placeholder="Your password" />
          </label>

          {error && <div className="rounded-xl bg-rose-50 px-3 py-2 text-sm text-rose-600 dark:bg-rose-900/30 dark:text-rose-300">{error}</div>}

          <div className="mt-2 flex justify-end gap-2">
            <button type="button" onClick={onClose} className="btn btn-ghost">Cancel</button>
            <button type="submit" disabled={busy || !password} className={`btn btn-danger ${busy ? 'is-loading' : ''}`}>
              Delete Customer
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
