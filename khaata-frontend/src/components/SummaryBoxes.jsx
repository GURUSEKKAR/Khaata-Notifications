import { Wallet, CircleCheck, Clock, AlertCircle } from 'lucide-react';
import { rupee } from '../api';

// The four summary cards. Consistent charcoal surface; color only on the amount
// (and the small dot) to carry financial meaning — amber owed, green paid, red overdue.
function Stat({ cls, label, value, Icon, overdueActive }) {
  return (
    <div className={`stat ${cls} ${overdueActive ? 'has' : ''} animate-in`}>
      <div className="label">
        <span className="dot" />
        <Icon size={15} className="icon" />
        {label}
      </div>
      <div className="value">{rupee(value)}</div>
    </div>
  );
}

export default function SummaryBoxes({ summary, selected }) {
  const scope = selected ? selected.name : 'All customers';
  const outstanding = selected ? selected.outstanding : summary?.totalOutstanding;
  const paid = selected ? selected.paid : summary?.totalPaid;
  const unpaid = selected ? selected.outstanding : summary?.unpaidDebt;
  const overdue = selected ? selected.overdue : summary?.overdue;
  const overdueActive = Number(overdue) > 0;

  return (
    <div>
      <h2 className="section-title" style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 10, fontWeight: 600 }}>
        Summary · {scope}
      </h2>
      <div className="dashboard-grid">
        <Stat cls="owed" label="Total Balance" value={outstanding} Icon={Wallet} />
        <Stat cls="paid" label="Paid" value={paid} Icon={CircleCheck} />
        <Stat cls="outstanding" label="Unpaid Debt" value={unpaid} Icon={Clock} />
        <Stat cls="overdue" label="Overdue" value={overdue} Icon={AlertCircle} overdueActive={overdueActive} />
      </div>
    </div>
  );
}
