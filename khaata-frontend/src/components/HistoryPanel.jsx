import { useEffect, useState } from 'react';
import { api, rupee, formatDateTime } from '../api';

// Full credit/repayment history for the selected customer, each row with its
// running balance — the digital version of a khaata page.
export default function HistoryPanel({ customer, refreshKey }) {
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!customer) return;
    let cancelled = false;
    setLoading(true);
    setError(null);
    api.getHistory(customer.id)
      .then((data) => { if (!cancelled) setEntries(data || []); })
      .catch((err) => { if (!cancelled) setError(err.message); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [customer, refreshKey]);

  if (!customer) return null;

  return (
    <div className="animate-in rounded-2xl border border-slate-200 bg-white/80 p-4 shadow-sm backdrop-blur dark:border-slate-700 dark:bg-slate-800/70">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="font-semibold text-slate-700 dark:text-slate-200">
          {customer.name}'s Credit History
        </h2>
        <span className="text-xs text-slate-400">{customer.phone}</span>
      </div>

      {loading && <p className="py-6 text-center text-sm text-slate-400">Loading history…</p>}
      {error && <p className="py-6 text-center text-sm text-rose-500">{error}</p>}

      {!loading && !error && entries.length === 0 && (
        <p className="py-6 text-center text-sm text-slate-400">No transactions yet for {customer.name}.</p>
      )}

      {!loading && !error && entries.length > 0 && (
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 text-left text-xs uppercase tracking-wide text-slate-400 dark:border-slate-700">
                <th className="py-2 pr-3">Date</th>
                <th className="py-2 pr-3">Type</th>
                <th className="py-2 pr-3">Note</th>
                <th className="py-2 pr-3 text-right">Amount</th>
                <th className="py-2 pl-3 text-right">Balance</th>
              </tr>
            </thead>
            <tbody>
              {entries.map((e) => {
                const isCredit = e.type === 'CREDIT';
                return (
                  <tr key={e.id} className="border-b border-slate-50 last:border-0 dark:border-slate-700/50">
                    <td className="py-2 pr-3 text-slate-500 dark:text-slate-400">{formatDateTime(e.createdAt)}</td>
                    <td className="py-2 pr-3">
                      <span className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${
                        isCredit ? 'bg-rose-100 text-rose-600 dark:bg-rose-900/40 dark:text-rose-300'
                                 : 'bg-emerald-100 text-emerald-600 dark:bg-emerald-900/40 dark:text-emerald-300'}`}>
                        {isCredit ? 'CREDIT' : 'REPAID'}
                      </span>
                      {e.overdue && (
                        <span className="ml-1 rounded-full bg-amber-100 px-2 py-0.5 text-[10px] font-semibold text-amber-700 dark:bg-amber-900/40 dark:text-amber-300">
                          OVERDUE
                        </span>
                      )}
                    </td>
                    <td className="py-2 pr-3 text-slate-500 dark:text-slate-400">
                      {e.note || '—'}
                      {isCredit && e.dueDate && (
                        <span className="block text-[11px] text-slate-400">due {e.dueDate}</span>
                      )}
                    </td>
                    <td className={`py-2 pr-3 text-right font-medium ${
                      isCredit ? 'text-rose-600 dark:text-rose-400' : 'text-emerald-600 dark:text-emerald-400'}`}>
                      {isCredit ? '+' : '−'}{rupee(e.amount)}
                    </td>
                    <td className="py-2 pl-3 text-right font-semibold text-slate-700 dark:text-slate-200">
                      {rupee(e.runningBalance)}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
