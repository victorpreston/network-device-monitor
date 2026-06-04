import { useEffect } from 'react';
import type { DeviceDetail } from '../types';

function fmt(iso: string | null) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('en-GB', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', second: '2-digit' });
}
function fmtDate(iso: string) {
  return new Date(iso).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
}
function timeAgo(iso: string | null) {
  if (!iso) return '—';
  const diff = Math.floor((Date.now() - new Date(iso).getTime()) / 1000);
  if (diff < 60)    return `${diff}s ago`;
  if (diff < 3600)  return `${Math.floor(diff / 60)}m ago`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
  return `${Math.floor(diff / 86400)}d ago`;
}

interface Props {
  device: DeviceDetail | null;
  loading: boolean;
  open: boolean;
  onClose: () => void;
  onSubmitReport: () => void;
}

export default function DetailDrawer({ device, loading, open, onClose, onSubmitReport }: Props) {
  useEffect(() => {
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [onClose]);

  const eff = device ? (device.stale ? 'stale' : (device.currentStatus?.toLowerCase() ?? 'stale')) : '';

  return (
    <div className={`drawer-overlay${open ? ' open' : ''}`} onClick={e => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="drawer">
        <div className="drawer-panel">
          <span className="drawer-handle" />

          <div className="drawer-header">
            <div>
              <div className="drawer-title">{device?.name ?? '—'}</div>
              <div className="drawer-subtitle">{device?.hostname ?? '—'}</div>
              {device && (
                <div className="drawer-status-row">
                  <div className={`drawer-status-badge ${eff}`}>
                    <div className={`led ${eff}`} />
                    {eff}
                  </div>
                  {device.stale && <span className="stale-chip">STALE</span>}
                </div>
              )}
            </div>
            <button className="drawer-close" onClick={onClose}>×</button>
          </div>

          <div className="drawer-body">
            {loading && <p style={{ fontFamily: 'var(--font-mono)', fontSize: 12, color: 'var(--text-3)', textAlign: 'center', padding: '40px 0' }}>Loading…</p>}

            {device && !loading && (
              <>
                <div className="detail-section">
                  <div className="detail-section-title">Device Info</div>
                  <div className="detail-grid">
                    {[
                      ['Type',       device.deviceType,               ''],
                      ['Site',       device.site,                     ''],
                      ['Hostname',   device.hostname,                 'accent'],
                      ['Registered', fmtDate(device.registeredAt),   ''],
                      ['Last Report',timeAgo(device.lastReportAt),    eff],
                      ['Status',     eff.toUpperCase(),               eff],
                    ].map(([key, val, cls]) => (
                      <div className="detail-item" key={key}>
                        <div className="detail-item-key">{key}</div>
                        <div className={`detail-item-val${cls ? ' ' + cls : ''}`} style={{ fontSize: 11 }}>{val}</div>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="detail-section">
                  <div className="detail-section-title">
                    Recent Status Reports ({device.recentReports.length} shown, newest first)
                  </div>
                  {device.recentReports.length === 0 ? (
                    <p style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--text-3)', padding: '16px 0' }}>
                      No reports submitted yet.
                    </p>
                  ) : (
                    <div className="report-timeline">
                      {device.recentReports.map(r => {
                        const rs = r.status.toLowerCase();
                        return (
                          <div className="report-item" key={r.id}>
                            <div className={`report-status-dot ${rs}`} />
                            <div className="report-content">
                              <div className="report-status-line">
                                <span className={`report-status-label ${rs}`}>{r.status}</span>
                                <span className="report-ts">{fmt(r.reportedAt)}</span>
                              </div>
                              {r.message && <div className="report-msg">{r.message}</div>}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>

                <button className="drawer-submit-btn" onClick={onSubmitReport}>
                  Submit Status Report
                </button>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
