import { rupee } from '../api';

// The four dashboard boxes. Shop-wide by default, or the selected customer's numbers.
function Box({ label, value, tone, icon }) {
  const tones = {
    blue: 'from-blue-50 to-blue-100 text-blue-700 dark:from-blue-950/60 dark:to-blue-900/40 dark:text-blue-300',
    green: 'from-emerald-50 to-emerald-100 text-emerald-700 dark:from-emerald-950/60 dark:to-emerald-900/40 dark:text-emerald-300',
    amber: 'from-amber-50 to-amber-100 text-amber-700 dark:from-amber-950/60 dark:to-amber-900/40 dark:text-amber-300',
    red: 'from-rose-50 to-rose-100 text-rose-700 dark:from-rose-950/60 dark:to-rose-900/40 dark:text-rose-300',
  };
  return (
    <div className={`animate-in rounded-2xl bg-gradient-to-br p-4 shadow-sm ${tones[tone]}`}>
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold uppercase tracking-wide opacity-70">{label}</span>
        <span className="text-lg opacity-60">{icon}</span>
      </div>
      <div className="mt-2 text-2xl font-bold">{rupee(value)}</div>
    </div>
  );
}

export default function SummaryBoxes({ summary, selected }) {
  const scope = selected ? selected.name : 'All customers';
  const outstanding = selected ? selected.outstanding : summary?.totalOutstanding;
  const paid = selected ? selected.paid : summary?.totalPaid;
  const unpaid = selected ? selected.outstanding : summary?.unpaidDebt;
  const overdue = selected ? selected.overdue : summary?.overdue;

  return (
    <div>
      <h2 className="mb-2 text-sm font-semibold text-slate-500 dark:text-slate-400">
        Summary · {scope}
      </h2>
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <Box label="Total Balance" value={outstanding} tone="blue" icon="💰" />
        <Box label="Paid" value={paid} tone="green" icon="✅" />
        <Box label="Unpaid Debt" value={unpaid} tone="amber" icon="📂" />
        <Box label="Overdue" value={overdue} tone="red" icon="⏰" />
      </div>
    </div>
  );
}
