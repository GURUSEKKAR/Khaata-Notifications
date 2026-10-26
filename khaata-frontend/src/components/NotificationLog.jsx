import { useEffect, useState } from 'react';
import { api, formatDateTime } from '../api';

// Shows the history of reminders the app has sent to customers — who, when, which
// channel, and whether it succeeded (with the failure reason when it didn't).
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

  const channelBadge = (ch) => {
    const map = {
      WHATSAPP: 'bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300',
      SMS: 'bg-sky-100 text-sky-700 dark:bg-sky-900/40 dark:text-sky-300',
      TELEGRAM: 'bg-cyan-100 text-cyan-700 dark:bg-cyan-900/40 dark:text-cyan-300',
      CONSOLE: 'bg-slate-100 text-slate-600 dark:bg-slate-700 dark:text-slate-300',
    };
    return map[ch] || map.CONSOLE;
  };

  return (
    <div className="animate-in rounded-2xl border border-slate-200 bg-white/80 p-4 shadow-sm backdrop-blur dark:border-slate-700 dark:bg-slate-800/70">
      <button
        onClick={() => setOpen((o) => !o)}
        className="flex w-full items-center justify-between"
      >
        <h2 className="flex items-center gap-2 font-semibold text-slate-700 dark:text-slate-200">
          <span>🔔 Notification Log</span>
          <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-500 dark:bg-slate-700 dark:text-slate-300">
            {entries.length}
          </span>
        </h2>
        <span className="text-slate-400">{open ? '▲' : '▼'}</span>
      </button>

      {open && (
        <div className="mt-3">
          {loading && <p className="py-4 text-center text-sm text-slate-400">Loading…</p>}
          {!loading && entries.length === 0 && (
            <p className="py-4 text-center text-sm text-slate-400">
              No reminders sent yet. Click “Send Reminders Now” or a customer’s “Remind”.
            </p>
          )}
          {!loading && entries.length > 0 && (
            <ul className="max-h-64 space-y-2 overflow-y-auto pr-1">
              {entries.map((e) => {
                const ok = e.status === 'SENT';
                return (
                  <li
                    key={e.id}
                    className="rounded-xl border border-slate-100 bg-white px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-900/40"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <span className="font-medium text-slate-700 dark:text-slate-200">
                          {nameById[e.customerId] || `Customer #${e.customerId}`}
                        </span>
                        <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${channelBadge(e.channel)}`}>
                          {e.channel}
                        </span>
                      </div>
                      <span
                        className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                          ok
                            ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300'
                            : 'bg-rose-100 text-rose-700 dark:bg-rose-900/40 dark:text-rose-300'
                        }`}
                      >
                        {e.status}
                      </span>
                    </div>
                    <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{e.message}</p>
                    {!ok && e.detail && (
                      <p className="mt-1 text-[11px] text-rose-500">{e.detail}</p>
                    )}
                    <p className="mt-1 text-[11px] text-slate-400">{formatDateTime(e.createdAt)}</p>
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
