import { useState } from 'react';
import { registerDevice } from '../api/devices';
import type { SiteItem, DeviceTypeItem } from '../types';

interface Props {
  open: boolean;
  sites: SiteItem[];
  deviceTypes: DeviceTypeItem[];
  onClose: () => void;
  onSuccess: (message: string) => void;
}

export default function RegisterModal({ open, sites, deviceTypes, onClose, onSuccess }: Props) {
  const [form, setForm] = useState({ name: '', deviceTypeId: '', hostname: '', siteId: '' });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm(f => ({ ...f, [k]: e.target.value }));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await registerDevice(form);
      setForm({ name: '', deviceTypeId: '', hostname: '', siteId: '' });
      onClose();
      onSuccess(`Device "${form.name}" registered successfully.`);
    } catch (err: any) {
      setError(err.response?.data?.errors?.[0]?.message ?? 'Registration failed. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  if (!open) return null;

  return (
    <div className={`modal-overlay${open ? ' open' : ''}`} onClick={e => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="modal">
        <div className="modal-header">
          <div className="modal-title">Register Device</div>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="form-error">{error}</div>}
            <div className="form-group">
              <label className="form-label">Device Name</label>
              <input className="form-input" placeholder="e.g. core-router-01" value={form.name} onChange={set('name')} required />
            </div>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Device Type</label>
                <select className="form-select" value={form.deviceTypeId} onChange={set('deviceTypeId')} required>
                  <option value="">Select type…</option>
                  {deviceTypes.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Hostname / IP</label>
                <input className="form-input" placeholder="e.g. 10.0.1.1" value={form.hostname} onChange={set('hostname')} required />
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">Site</label>
              <select className="form-select" value={form.siteId} onChange={set('siteId')} required>
                <option value="">Select site…</option>
                {sites.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn-cancel" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn-submit" disabled={submitting}>
              {submitting ? 'Registering…' : 'Register Device'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
