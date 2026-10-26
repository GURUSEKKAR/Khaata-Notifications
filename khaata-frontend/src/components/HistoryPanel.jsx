import { useEffect, useState } from 'react';
import { api, rupee, formatDateTime } from '../api';

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

  const th = { padding: '8px 12px 8px 0', textAlign: 'left', fontSize: 11, textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--muted)', borderBottom: '1px solid var(--border)' };
  const td = { padding: '10px 12px 10px 0', borderBottom: '1px solid var(--border)', color: 'var(--text)' };

  return (
    <div className="card animate-in">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
        <h2 style={{ fontSize: 15, fontWeight: 600, margin: 0 }}>{customer.name}'s Credit History</h2>
        <span style={{ fontSize: 12, color: 'var(--muted)' }}>{customer.phone}</span>
      </div>

      {loading && <p style={{ textAlign: 'center', color: 'var(--muted)', padding: '20px 0' }}>Loading history…</p>}
      {error && <p style={{ textAlign: 'center', color: 'var(--overdue)', padding: '20px 0' }}>{error}</p>}
      {!loading && !error && entries.length === 0 && (
        <p style={{ textAlign: 'center', color: 'var(--muted)', padding: '20px 0' }}>No transactions yet for {customer.name}.</p>
      )}

      {!loading && !error && entries.length > 0 && (
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
            <thead>
              <tr>
                <th style={th}>Date</th><th style={th}>Type</th><th style={th}>Note</th>
                <th style={{ ...th, textAlign: 'right' }}>Amount</th>
                <th style={{ ...th, textAlign: 'right', paddingRight: 0 }}>Balance</th>
              </tr>
            </thead>
            <tbody>
              {entries.map((e) => {
                const isCredit = e.type === 'CREDIT';
                return (
                  <tr key={e.id}>
                    <td style={{ ...td, color: 'var(--muted)' }}>{formatDateTime(e.createdAt)}</td>
                    <td style={td}>
                      <span className={isCredit ? 'badge badge-credit' : 'badge badge-paid'}>{isCredit ? 'CREDIT' : 'REPAID'}</span>
                      {e.overdue && <span className="badge badge-overdue" style={{ marginLeft: 4 }}>OVERDUE</span>}
                    </td>
                    <td style={{ ...td, color: 'var(--muted)' }}>
                      {e.note || '—'}
                      {isCredit && e.dueDate && <span style={{ display: 'block', fontSize: 11, color: 'var(--disabled)' }}>due {e.dueDate}</span>}
                    </td>
                    <td style={{ ...td, textAlign: 'right', fontWeight: 600, color: isCredit ? 'var(--owed)' : 'var(--paid)', fontVariantNumeric: 'tabular-nums' }}>
                      {isCredit ? '+' : '−'}{rupee(e.amount)}
                    </td>
                    <td style={{ ...td, textAlign: 'right', paddingRight: 0, fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}>
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
