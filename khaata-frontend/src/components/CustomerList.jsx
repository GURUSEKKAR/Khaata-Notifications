import { Bell, Pencil, Trash2, UserPlus } from 'lucide-react';
import { rupee } from '../api';

export default function CustomerList({ customers, selectedId, onSelect, onAddNew, onEdit, onDelete, onRemind }) {
  const initials = (name) => name.trim().slice(0, 1).toUpperCase();

  return (
    <aside className="card customers-panel" style={{ padding: 0, display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '14px 18px', borderBottom: '1px solid var(--border)' }}>
        <h2 style={{ fontSize: 15, fontWeight: 600, margin: 0 }}>Customers</h2>
        <button onClick={onAddNew} className="btn btn-primary" style={{ minHeight: 34, padding: '0 14px', fontSize: 13 }}>
          <UserPlus size={15} /> New
        </button>
      </div>

      <div style={{ flex: 1, overflowY: 'auto', padding: '0 18px' }}>
        {customers.length === 0 && (
          <p style={{ color: 'var(--muted)', fontSize: 14, padding: '16px 0' }}>No customers yet. Add one to begin.</p>
        )}

        {customers.map((c) => {
          const active = c.id === selectedId;
          const owes = Number(c.outstanding) > 0;
          const isOverdue = Number(c.overdue) > 0;
          return (
            <div key={c.id} className="customer-row"
              style={active ? { background: 'var(--raised)', borderRadius: 12, paddingLeft: 10, paddingRight: 10, marginLeft: -10, marginRight: -10 } : undefined}>
              <button onClick={() => onSelect(active ? null : c.id)} className="customer-info"
                style={{ background: 'transparent', border: 0, cursor: 'pointer', padding: 0, textAlign: 'left' }}>
                <span className="avatar" style={isOverdue ? { borderColor: 'var(--overdue)' } : undefined}>{initials(c.name)}</span>
                <span style={{ minWidth: 0 }}>
                  <span style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 0 }}>
                    <span className="name">{c.name}</span>
                    {isOverdue && <span className="badge badge-overdue">OVERDUE</span>}
                  </span>
                  <span style={{ display: 'block', fontSize: 12, color: 'var(--muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{c.phone}</span>
                </span>
              </button>

              <div style={{ display: 'flex', alignItems: 'center', gap: 8, flex: '0 0 auto' }}>
                <span className={`amt ${owes ? '' : 'clear'}`}>{rupee(c.outstanding)}</span>
                <div style={{ display: 'flex', gap: 2 }}>
                  <button title="Remind now" className="btn btn-ghost btn-icon" style={{ minHeight: 32, minWidth: 32 }}
                    onClick={(e) => { e.stopPropagation(); onRemind(c); }}><Bell size={15} /></button>
                  <button title="Edit" className="btn btn-ghost btn-icon" style={{ minHeight: 32, minWidth: 32 }}
                    onClick={(e) => { e.stopPropagation(); onEdit(c); }}><Pencil size={15} /></button>
                  <button title="Delete" className="btn btn-ghost btn-icon" style={{ minHeight: 32, minWidth: 32 }}
                    onClick={(e) => { e.stopPropagation(); onDelete(c); }}><Trash2 size={15} /></button>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </aside>
  );
}
