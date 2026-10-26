import { useEffect, useState } from 'react';
import { Bell, ChevronDown, ChevronUp } from 'lucide-react';
import { api, formatDateTime } from '../api';

export default function NotificationLog({ refreshKey, customers }) {
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(true);

  const nameById = Object.fromEntries((customers || []).map((c) => [c.id, c.name]));

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    api.getReminderLog()
      .then((d) => { if (!cancelled) setEntries(d || []); })
      .catch(() => {})
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [refreshKey]);

  const badgeClass = (ch) => ({
    WHATSAPP: 'badge badge-paid', SMS: 'badge', TELEGRAM: 'badge badge-credit', CONSOLE: 'badge',
  }[ch] || 'badge');

  return (
    <div className="card animate-in">
      <button onClick={() => setOpen((o) => !o)}
        style={{ display: 'flex', width: '100%', alignItems: 'center', justifyContent: 'space-between', background: 'transparent', border: 0, cursor: 'pointer', color: 'var(--text)', padding: 0 }}>
        <span style={{ display: 'flex', alignItems: 'center', gap: 8, fontWeight: 600, fontSize: 15 }}>
          <Bell size={16} className="icon" /> Notification Log
          <span className="badge">{entries.length}</span>
        </span>
        {open ? <ChevronUp size={16} className="icon" /> : <ChevronDown size={16} className="icon" />}
      </button>

      {open && (
        <div style={{ marginTop: 12 }}>
          {loading && <p style={{ textAlign: 'center', color: 'var(--muted)', padding: '12px 0' }}>Loading…</p>}
          {!loading && entries.length === 0 && (
            <p style={{ textAlign: 'center', color: 'var(--muted)', padding: '12px 0', fontSize: 13 }}>
              No reminders sent yet. Click “Send Reminders Now” or a customer’s bell.
            </p>
          )}
          {!loading && entries.length > 0 && (
            <ul style={{ listStyle: 'none', margin: 0, padding: 0, display: 'flex', flexDirection: 'column', gap: 8, maxHeight: 260, overflowY: 'auto' }}>
              {entries.map((e) => {
                const ok = e.status === 'SENT';
                return (
                  <li key={e.id} style={{ border: '1px solid var(--border)', borderRadius: 12, background: 'var(--raised)', padding: '10px 12px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <span style={{ fontWeight: 600 }}>{nameById[e.customerId] || `Customer #${e.customerId}`}</span>
                        <span className={badgeClass(e.channel)}>{e.channel}</span>
                      </span>
                      <span className={ok ? 'badge badge-paid' : 'badge badge-overdue'}>{e.status}</span>
                    </div>
                    <p style={{ margin: '6px 0 0', fontSize: 12, color: 'var(--muted)' }}>{e.message}</p>
                    {!ok && e.detail && <p style={{ margin: '4px 0 0', fontSize: 11, color: 'var(--overdue)' }}>{e.detail}</p>}
                    <p style={{ margin: '4px 0 0', fontSize: 11, color: 'var(--disabled)' }}>{formatDateTime(e.createdAt)}</p>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
