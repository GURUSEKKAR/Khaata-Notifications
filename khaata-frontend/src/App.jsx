import { useCallback, useEffect, useState } from 'react';
import { Bell, LogOut, UserCog } from 'lucide-react';
import { api, auth } from './api';
import ActionPanel from './components/ActionPanel';
import SummaryBoxes from './components/SummaryBoxes';
import CustomerList from './components/CustomerList';
import NewCustomerModal from './components/NewCustomerModal';
import EditCustomerModal from './components/EditCustomerModal';
import DeleteCustomerModal from './components/DeleteCustomerModal';
import HistoryPanel from './components/HistoryPanel';
import NotificationLog from './components/NotificationLog';
import ProfileModal from './components/ProfileModal';
import Login from './components/Login';

export default function App() {
  const [loggedIn, setLoggedIn] = useState(auth.isLoggedIn());
  const [summary, setSummary] = useState(null);
  const [customers, setCustomers] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [toast, setToast] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editCustomer, setEditCustomer] = useState(null);
  const [deleteCustomer, setDeleteCustomer] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [logKey, setLogKey] = useState(0);
  const [remindBusy, setRemindBusy] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);

  useEffect(() => {
    const onUnauthorized = () => setLoggedIn(false);
    window.addEventListener('khaata-unauthorized', onUnauthorized);
    return () => window.removeEventListener('khaata-unauthorized', onUnauthorized);
  }, []);

  const load = useCallback(async () => {
    try {
      const [s, c] = await Promise.all([api.getSummary(), api.getCustomers()]);
      setSummary(s);
      setCustomers(c);
    } catch (err) { flash(err.message, 'error'); }
  }, []);

  useEffect(() => { if (loggedIn) load(); }, [loggedIn, load]);

  function flash(message, type = 'success') {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  }

  function logout() { auth.clear(); setLoggedIn(false); setSelectedId(null); }

  const selected = customers.find((c) => c.id === selectedId) || null;
  const anyOverdue = Number(summary?.overdue) > 0;

  async function afterChange(msg) {
    flash(msg, 'success');
    setRefreshKey((k) => k + 1);
    await load();
  }

  async function runReminders() {
    setRemindBusy(true);
    try {
      const res = await api.runReminders();
      flash(`Reminders run — ${res.sent} message(s) sent.`, 'success');
      setLogKey((k) => k + 1);
      await load();
    } catch (err) { flash(err.message, 'error'); } finally { setRemindBusy(false); }
  }

  async function remindOne(c) {
    try {
      const res = await api.remindCustomer(c.id);
      flash(res.sent ? `Reminder sent to ${c.name}.` : `Reminder for ${c.name} not delivered — check the log.`, res.sent ? 'success' : 'error');
      setLogKey((k) => k + 1);
    } catch (err) { flash(err.message, 'error'); }
  }

  if (!loggedIn) {
    return <Login onLoggedIn={(u) => { setLoggedIn(true); flash(`Welcome, ${u.ownerName || u.shopName}.`); }} />;
  }

  return (
    <div className="app-bg">
      <div style={{ margin: '0 auto', maxWidth: 1200, padding: 16, display: 'flex', flexDirection: 'column', minHeight: '100%' }}>
        {/* Header */}
        <header style={{ marginBottom: 16, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{ width: 44, height: 44, display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: 14, background: 'var(--raised)', border: '1px solid var(--border)', fontSize: 20 }}>📒</div>
            <div>
              <h1 style={{ fontSize: 22, fontWeight: 700, margin: 0 }}>Khaata</h1>
              <p style={{ fontSize: 13, color: 'var(--muted)', margin: 0 }}>Digital udhaar book — track credit, chase repayments.</p>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <button onClick={runReminders} disabled={remindBusy}
              className={`btn btn-outline ${anyOverdue ? 'alert' : ''} ${remindBusy ? 'is-loading' : ''}`}>
              <Bell size={16} /> Send Reminders Now
            </button>
            <button onClick={() => setProfileOpen(true)} className="btn btn-ghost"><UserCog size={16} /> Profile</button>
            <button onClick={logout} className="btn btn-ghost"><LogOut size={16} /> Logout</button>
          </div>
        </header>

        {/* Body: 70 / 30 */}
        <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 7fr) minmax(0, 3fr)', gap: 16, flex: 1, alignItems: 'start' }} className="layout-grid">
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16, minWidth: 0 }}>
            <ActionPanel customers={customers} selectedId={selectedId} onDone={afterChange} onError={(m) => flash(m, 'error')} />
            <SummaryBoxes summary={summary} selected={selected} />

            {selected ? (
              <>
                <div className="card" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, padding: '12px 16px', flexWrap: 'wrap' }}>
                  <span style={{ fontSize: 14, color: 'var(--muted)' }}>Viewing <b style={{ color: 'var(--text)' }}>{selected.name}</b>'s khaata.</span>
                  <button onClick={() => setSelectedId(null)} className="btn btn-outline" style={{ minHeight: 36, padding: '0 14px', fontSize: 13 }}>Show shop-wide totals</button>
                </div>
                <HistoryPanel customer={selected} refreshKey={refreshKey} />
              </>
            ) : (
              <div className="card" style={{ textAlign: 'center', color: 'var(--muted)', fontSize: 14, padding: 24 }}>
                Select a customer on the right to see their full credit history.
              </div>
            )}

            <NotificationLog refreshKey={logKey} customers={customers} />
          </div>

          <div style={{ minWidth: 0, position: 'sticky', top: 16, maxHeight: 'calc(100vh - 32px)' }}>
            <CustomerList customers={customers} selectedId={selectedId}
              onSelect={setSelectedId} onAddNew={() => setModalOpen(true)}
              onEdit={(c) => setEditCustomer(c)} onDelete={(c) => setDeleteCustomer(c)} onRemind={remindOne} />
          </div>
        </div>
      </div>

      <NewCustomerModal open={modalOpen} onClose={() => setModalOpen(false)}
        onCreated={async (msg) => { setModalOpen(false); await afterChange(msg); }} onError={(m) => flash(m, 'error')} />
      <EditCustomerModal customer={editCustomer} onClose={() => setEditCustomer(null)}
        onSaved={async (msg) => { setEditCustomer(null); await afterChange(msg); }} onError={(m) => flash(m, 'error')} />
      <DeleteCustomerModal customer={deleteCustomer} onClose={() => setDeleteCustomer(null)}
        onDeleted={async (msg) => { const id = deleteCustomer?.id; setDeleteCustomer(null); if (id === selectedId) setSelectedId(null); await afterChange(msg); }}
        onError={() => {}} />

      <ProfileModal open={profileOpen} onClose={() => setProfileOpen(false)}
        onSaved={(msg) => { setProfileOpen(false); flash(msg); }}
        onError={(m) => flash(m, 'error')}
        onDeleted={() => { setProfileOpen(false); setLoggedIn(false); flash('Account deleted.'); }} />

      {toast && (
        <div className="animate-in" style={{
          position: 'fixed', bottom: 16, right: 16, padding: '10px 16px', borderRadius: 12, fontWeight: 600, fontSize: 14,
          color: toast.type === 'error' ? '#2B0612' : '#062416',
          background: toast.type === 'error' ? 'var(--overdue)' : 'var(--paid)',
          boxShadow: '0 10px 30px rgba(0,0,0,0.4)',
        }}>{toast.message}</div>
      )}
    </div>
  );
}
