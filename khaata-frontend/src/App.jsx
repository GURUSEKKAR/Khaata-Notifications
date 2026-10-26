import { useCallback, useEffect, useState } from 'react';
import { api, auth } from './api';
import { useTheme } from './useTheme';
import ActionPanel from './components/ActionPanel';
import SummaryBoxes from './components/SummaryBoxes';
import CustomerList from './components/CustomerList';
import NewCustomerModal from './components/NewCustomerModal';
import EditCustomerModal from './components/EditCustomerModal';
import DeleteCustomerModal from './components/DeleteCustomerModal';
import HistoryPanel from './components/HistoryPanel';
import NotificationLog from './components/NotificationLog';
import Login from './components/Login';

export default function App() {
  const { theme, toggle } = useTheme();
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
    } catch (err) {
      flash(err.message, 'error');
    }
  }, []);

  useEffect(() => { if (loggedIn) load(); }, [loggedIn, load]);

  function flash(message, type = 'success') {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  }

  function logout() {
    auth.clear();
    setLoggedIn(false);
    setSelectedId(null);
  }

  const selected = customers.find((c) => c.id === selectedId) || null;

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
    } catch (err) {
      flash(err.message, 'error');
    } finally {
      setRemindBusy(false);
    }
  }

  async function remindOne(c) {
    try {
      const res = await api.remindCustomer(c.id);
      flash(res.sent ? `Reminder sent to ${c.name}.` : `Reminder for ${c.name} was not delivered — check the log.`,
        res.sent ? 'success' : 'error');
      setLogKey((k) => k + 1);
    } catch (err) {
      flash(err.message, 'error');
    }
  }

  if (!loggedIn) {
    return <Login theme={theme} onToggleTheme={toggle}
      onLoggedIn={(u) => { setLoggedIn(true); flash(`Welcome, ${u.displayName || u.username}.`); }} />;
  }

  return (
    <div className="app-bg min-h-full">
      <div className="mx-auto flex min-h-full max-w-7xl flex-col p-4">
        {/* Header */}
        <header className="mb-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-brand-500 text-xl shadow-lg">📒</div>
            <div>
              <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">Khaata</h1>
              <p className="text-sm text-slate-400">Digital udhaar book — track credit, chase repayments.</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button onClick={toggle} aria-label="Toggle theme" className="btn btn-ghost btn-icon">
              {theme === 'dark' ? '☀️' : '🌙'}
            </button>
            <button onClick={runReminders} disabled={remindBusy}
              className={`btn btn-danger ${remindBusy ? 'is-loading' : ''}`}>
              Send Reminders Now
            </button>
            <button onClick={logout} className="btn btn-ghost">
              Logout
            </button>
          </div>
        </header>

        {/* Body: 70 / 30 split */}
        <div className="grid flex-1 grid-cols-1 gap-4 lg:grid-cols-10">
          {/* LEFT */}
          <div className="flex flex-col gap-4 lg:col-span-7">
            <ActionPanel customers={customers} selectedId={selectedId}
              onDone={afterChange} onError={(m) => flash(m, 'error')} />
            <SummaryBoxes summary={summary} selected={selected} />

            {selected ? (
              <>
                <div className="flex items-center justify-between rounded-2xl border border-slate-200 bg-white/80 px-4 py-2 text-sm text-slate-500 shadow-sm backdrop-blur dark:border-slate-700 dark:bg-slate-800/70 dark:text-slate-400">
                  <span>Viewing <span className="font-semibold text-slate-700 dark:text-slate-200">{selected.name}</span>'s khaata.</span>
                  <button onClick={() => setSelectedId(null)} className="btn btn-outline" style={{ minHeight: '36px', padding: '0 14px', fontSize: '13px' }}>
                    Show shop-wide totals
                  </button>
                </div>
                <HistoryPanel customer={selected} refreshKey={refreshKey} />
              </>
            ) : (
              <div className="rounded-2xl border border-slate-200 bg-white/60 p-6 text-center text-sm text-slate-400 shadow-sm backdrop-blur dark:border-slate-700 dark:bg-slate-800/50">
                Select a customer on the right to see their full credit history.
              </div>
            )}

            <NotificationLog refreshKey={logKey} customers={customers} />
          </div>

          {/* RIGHT */}
          <div className="lg:col-span-3">
            <CustomerList customers={customers} selectedId={selectedId}
              onSelect={setSelectedId} onAddNew={() => setModalOpen(true)}
              onEdit={(c) => setEditCustomer(c)} onDelete={(c) => setDeleteCustomer(c)}
              onRemind={remindOne} />
          </div>
        </div>
      </div>

      <NewCustomerModal open={modalOpen} onClose={() => setModalOpen(false)}
        onCreated={async (msg) => { setModalOpen(false); await afterChange(msg); }}
        onError={(m) => flash(m, 'error')} />

      <EditCustomerModal customer={editCustomer} onClose={() => setEditCustomer(null)}
        onSaved={async (msg) => { setEditCustomer(null); await afterChange(msg); }}
        onError={(m) => flash(m, 'error')} />

      <DeleteCustomerModal customer={deleteCustomer} onClose={() => setDeleteCustomer(null)}
        onDeleted={async (msg) => {
          const deletedId = deleteCustomer?.id;
          setDeleteCustomer(null);
          if (deletedId === selectedId) setSelectedId(null);
          await afterChange(msg);
        }}
        onError={() => {}} />

      {toast && (
        <div className={`animate-in fixed bottom-4 right-4 rounded-xl px-4 py-2 text-sm font-medium text-white shadow-lg ${
          toast.type === 'error' ? 'bg-rose-500' : 'bg-emerald-500'}`}>
          {toast.message}
        </div>
      )}
    </div>
  );
}
