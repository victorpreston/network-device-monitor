import type { DeviceListItem } from '../types';

function timeAgo(iso: string | null): string {
  if (!iso) return '—';
  const diff = Math.floor((Date.now() - new Date(iso).getTime()) / 1000);
  if (diff < 60)    return `${diff}s ago`;
  if (diff < 3600)  return `${Math.floor(diff / 60)}m ago`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
  return `${Math.floor(diff / 86400)}d ago`;
}

function trunc(s: string, n: number) {
  return s.length > n ? s.slice(0, n) + '…' : s;
}

interface Props {
  device: DeviceListItem;
  index: number;
  onClick: () => void;
}

export default function DeviceCard({ device, index, onClick }: Props) {
  const eff = device.stale ? 'stale' : (device.currentStatus?.toLowerCase() ?? 'stale');
  const delay = Math.min(index * 0.03, 0.3);

  return (
    <div
      className={`device-card ${eff}`}
      style={{ animationDelay: `${delay}s` }}
      onClick={onClick}
    >
      <div className="card-top">
        <span className="card-type-badge">{device.deviceType}</span>
        <div className="status-led">
          <div className={`led ${eff}`} />
          <span className={`status-text ${eff}`}>{eff}</span>
        </div>
      </div>

      <div className="card-name">{device.name}</div>
      <div className="card-hostname">{device.hostname}</div>

      <div className="card-meta">
        <div className="meta-row">
          <span className="meta-key">Last seen</span>
          <span className="meta-val">{timeAgo(device.lastReportAt)}</span>
        </div>
        {device.stale && (
          <div className="meta-row">
            <span className="meta-key">Stale</span>
            <span className="meta-val stale-flag">No report &gt;15m</span>
          </div>
        )}
      </div>

      <div className="card-footer">
        <span className="card-site">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
            <circle cx="12" cy="10" r="3" />
          </svg>
          {trunc(device.site, 22)}
        </span>
        <button className="card-detail-btn" onClick={e => { e.stopPropagation(); onClick(); }}>
          Details
        </button>
      </div>
    </div>
  );
}
