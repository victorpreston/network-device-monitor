type Filter = 'all' | 'online' | 'degraded' | 'offline' | 'stale';

interface Props {
  total: number; online: number; degraded: number; offline: number; stale: number;
  active: Filter;
  onFilter: (f: Filter) => void;
}

export default function SummaryStrip({ total, online, degraded, offline, stale, active, onFilter }: Props) {
  const cards: { key: Filter; label: string; value: number; sub: string }[] = [
    { key: 'all',      label: 'Total',    value: total,    sub: 'registered devices' },
    { key: 'online',   label: 'Online',   value: online,   sub: 'operational' },
    { key: 'degraded', label: 'Degraded', value: degraded, sub: 'attention needed' },
    { key: 'offline',  label: 'Offline',  value: offline,  sub: 'unreachable' },
    { key: 'stale',    label: 'Stale',    value: stale,    sub: 'no report >15min' },
  ];

  return (
    <div className="summary-strip">
      {cards.map(c => (
        <div
          key={c.key}
          className={`summary-card ${c.key}${active === c.key ? ' active-filter' : ''}`}
          onClick={() => onFilter(c.key)}
        >
          <div className="summary-label">{c.label}</div>
          <div className={`summary-val ${c.key}`}>{c.value}</div>
          <div className="summary-sub">{c.sub}</div>
        </div>
      ))}
    </div>
  );
}
