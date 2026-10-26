import { rupee } from '../api';

// Right-hand panel: customers with balances. Each row: select, remind, edit, delete.
export default function CustomerList({ customers, selectedId, onSelect, onAddNew, onEdit, onDelete, onRemind }) {
  const initials = (name) => name.trim().slice(0, 1).toUpperCase();

  return (
    <aside className="flex h-full flex-col rounded-2xl border border-slate-200 bg-white/80 shadow-sm backdrop-blur dark:border-slate-700 dark:bg-slate-800/70">
      <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3 dark:border-slate-700">
        <h2 className="font-semibold text-slate-700 dark:text-slate-200">Customers</h2>
        <button onClick={onAddNew}
          className="rounded-lg bg-brand-500 px-3 py-1 text-sm font-medium text-white shadow-sm hover:bg-brand-600">
          + New
        </button>
      </div>

      <div className="flex-1 overflow-y-auto">
        {customers.length === 0 && (
          <p className="p-4 text-sm text-slate-400">No customers yet. Add one to begin.</p>
        )}
        <ul className="divide-y divide-slate-100 dark:divide-slate-700">
          {customers.map((c) => {
            const active = c.id === selectedId;
            const owes = Number(c.outstanding) > 0;
            const isOverdue = Number(c.overdue) > 0;
            return (
              <li key={c.id}
                className={`group transition ${active ? 'bg-brand-50 dark:bg-brand-500/10' : 'hover:bg-slate-50 dark:hover:bg-slate-700/40'}`}>
                <div className="flex items-center gap-3 px-4 py-3">
                  <button onClick={() => onSelect(active ? null : c.id)}
                    className="flex flex-1 items-center gap-3 text-left">
                    <span className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-sm font-bold text-white ${
                      isOverdue ? 'bg-rose-500' : owes ? 'bg-amber-500' : 'bg-emerald-500'
                    }`}>
                      {initials(c.name)}
                    </span>
                    <span className="min-w-0">
                      <span className="flex items-center gap-2 font-medium text-slate-800 dark:text-slate-100">
                        <span className="truncate">{c.name}</span>
                        {isOverdue && (
                          <span className="rounded-full bg-rose-100 px-2 py-0.5 text-[10px] font-semibold text-rose-600 dark:bg-rose-900/40 dark:text-rose-300">
                            OVERDUE
                          </span>
                        )}
                      </span>
                      <span className="block truncate text-xs text-slate-400">{c.phone}</span>
                    </span>
                  </button>

                  <div className="flex flex-col items-end gap-1">
                    <span className={`text-sm font-semibold ${owes ? 'text-rose-600 dark:text-rose-400' : 'text-emerald-600 dark:text-emerald-400'}`}>
                      {rupee(c.outstanding)}
                    </span>
                    <div className="flex gap-1 opacity-0 transition group-hover:opacity-100">
                      <button title="Remind now" onClick={(e) => { e.stopPropagation(); onRemind(c); }}
                        className="rounded-md p-1 text-slate-400 hover:bg-brand-100 hover:text-brand-600 dark:hover:bg-brand-500/20">🔔</button>
                      <button title="Edit" onClick={(e) => { e.stopPropagation(); onEdit(c); }}
                        className="rounded-md p-1 text-slate-400 hover:bg-slate-200 hover:text-slate-700 dark:hover:bg-slate-600">✏️</button>
                      <button title="Delete" onClick={(e) => { e.stopPropagation(); onDelete(c); }}
                        className="rounded-md p-1 text-slate-400 hover:bg-rose-100 hover:text-rose-600 dark:hover:bg-rose-500/20">🗑️</button>
                    </div>
                  </div>
                </div>
              </li>
            );
          })}
        </ul>
      </div>
    </aside>
  );
}
