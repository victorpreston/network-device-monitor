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
  devices: DeviceListItem[];
  visible: boolean;
  onSelect: (id: string) => void;
}

export default function DeviceTable({ devices, visible, onSelect }: Props) {
  return (
    <div className={`devices-table-wrap${visible ? ' visible' : ''}`}>
      <table className="devices-table">
        <thead>
          <tr>
            {['Device Name', 'Type', 'Hostname / IP', 'Site', 'Status', 'Last Report', 'Stale'].map(h => (
              <th key={h}>{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {devices.length === 0 ? (
            <tr>
              <td colSpan={7} style={{ textAlign: 'center', padding: '48px', color: 'var(--text-3)', fontSize: '13px' }}>
                No devices match current filter
              </td>
            </tr>
          ) : devices.map(d => {
            const eff = d.stale ? 'stale' : (d.currentStatus?.toLowerCase() ?? 'stale');
            return (
              <tr key={d.id} onClick={() => onSelect(d.id)}>
                <td>{d.name}</td>
                <td>{d.deviceType}</td>
                <td>{d.hostname}</td>
                <td>{trunc(d.site, 28)}</td>
                <td>
                  <div className="tbl-led-cell">
                    <div className={`led ${eff}`} style={{ width: 7, height: 7 }} />
                    <span className={`tbl-status-pill ${eff}`}>{eff}</span>
                  </div>
                </td>
                <td>{timeAgo(d.lastReportAt)}</td>
                <td>{d.stale ? <span className="stale-chip">STALE</span> : <span style={{ color: 'var(--text-3)' }}>—</span>}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
