import { useState } from 'react';
import { submitReport } from '../api/devices';
import type { DeviceStatus } from '../types';

interface Props {
  open: boolean;
  deviceId: string;
  deviceName: string;
  onClose: () => void;
  onSuccess: (message: string) => void;
}

export default function SubmitReportModal({ open, deviceId, deviceName, onClose, onSuccess }: Props) {
  const [status, setStatus] = useState<DeviceStatus>('ONLINE');
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await submitReport(deviceId, status, message);
      setMessage('');
      setStatus('ONLINE');
      onClose();
      onSuccess(`Report submitted: ${status}`);
    } catch (err: any) {
      setError(err.response?.data?.errors?.[0]?.message ?? 'Submission failed.');
    } finally {
      setSubmitting(false);
    }
  };

  if (!open) return null;

  return (
    <div className={`modal-overlay${open ? ' open' : ''}`} onClick={e => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="modal" style={{ maxWidth: 400 }}>
        <div className="modal-header">
          <div className="modal-title">Submit Report</div>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            <p style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--text-3)', marginTop: -4 }}>
              {deviceName}
            </p>
            {error && <div className="form-error">{error}</div>}
            <div className="form-group">
              <label className="form-label">Status</label>
              <select className="form-select" value={status} onChange={e => setStatus(e.target.value as DeviceStatus)}>
                <option value="ONLINE">ONLINE</option>
                <option value="DEGRADED">DEGRADED</option>
                <option value="OFFLINE">OFFLINE</option>
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Message (optional)</label>
              <input
                className="form-input"
                placeholder="e.g. All interfaces up, CPU 12%"
                value={message}
                onChange={e => setMessage(e.target.value)}
              />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn-cancel" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn-submit" disabled={submitting}>
              {submitting ? 'Submitting…' : 'Submit'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
