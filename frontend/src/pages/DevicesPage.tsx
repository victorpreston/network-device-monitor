import { useCallback, useEffect, useState } from 'react';
import { getDevices, getDevice } from '../api/devices';
import { getSites } from '../api/sites';
import { getDeviceTypes } from '../api/deviceTypes';
import Navbar from '../components/Navbar';
import SummaryStrip from '../components/SummaryStrip';
import DeviceCard from '../components/DeviceCard';
import DeviceTable from '../components/DeviceTable';
import DetailDrawer from '../components/DetailDrawer';
import RegisterModal from '../components/RegisterModal';
import SubmitReportModal from '../components/SubmitReportModal';
import Toast from '../components/Toast';
import type { DeviceListItem, DeviceDetail, SiteItem, DeviceTypeItem } from '../types';

type Filter = 'all' | 'online' | 'degraded' | 'offline' | 'stale';
type SortKey = 'name' | 'status' | 'lastReport' | 'type';
type View = 'grid' | 'table';

function effectiveStatus(d: DeviceListItem): string {
  return d.stale ? 'stale' : (d.currentStatus?.toLowerCase() ?? 'stale');
}

export default function DevicesPage() {
  const [devices, setDevices]         = useState<DeviceListItem[]>([]);
  const [loading, setLoading]         = useState(true);
  const [search, setSearch]           = useState('');
  const [filter, setFilter]           = useState<Filter>('all');
  const [sort, setSort]               = useState<SortKey>('name');
  const [view, setView]               = useState<View>('grid');

  const [drawerOpen, setDrawerOpen]         = useState(false);
  const [drawerDevice, setDrawerDevice]     = useState<DeviceDetail | null>(null);
  const [drawerLoading, setDrawerLoading]   = useState(false);

  const [showRegister, setShowRegister]     = useState(false);
  const [showReport, setShowReport]         = useState(false);

  const [sites, setSites]               = useState<SiteItem[]>([]);
  const [deviceTypes, setDeviceTypes]   = useState<DeviceTypeItem[]>([]);

  const [toast, setToast] = useState({ visible: false, message: '', type: 'success' as 'success' | 'error' });

  const showToast = (message: string, type: 'success' | 'error' = 'success') =>
    setToast({ visible: true, message, type });

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getDevices();
      setDevices(res.data.data ?? []);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
    getSites().then(r => setSites(r.data.data ?? []));
    getDeviceTypes().then(r => setDeviceTypes(r.data.data ?? []));
  }, [load]);

  const openDrawer = async (id: string) => {
    setDrawerOpen(true);
    setDrawerLoading(true);
    setDrawerDevice(null);
    try {
      const res = await getDevice(id);
      setDrawerDevice(res.data.data);
    } finally {
      setDrawerLoading(false);
    }
  };

  const closeDrawer = () => {
    setDrawerOpen(false);
    setDrawerDevice(null);
    document.body.style.overflow = '';
  };

  useEffect(() => {
    if (drawerOpen) document.body.style.overflow = 'hidden';
    else document.body.style.overflow = '';
  }, [drawerOpen]);

  // Counts
  const counts = {
    total:    devices.length,
    online:   devices.filter(d => effectiveStatus(d) === 'online').length,
    degraded: devices.filter(d => effectiveStatus(d) === 'degraded').length,
    offline:  devices.filter(d => effectiveStatus(d) === 'offline').length,
    stale:    devices.filter(d => effectiveStatus(d) === 'stale').length,
  };

  // Filter + search + sort
  const filtered = devices
    .filter(d => {
      const matchFilter = filter === 'all' || effectiveStatus(d) === filter;
      const q = search.toLowerCase();
      const matchSearch = !q || [d.name, d.hostname, d.deviceType, d.site].some(v => v.toLowerCase().includes(q));
      return matchFilter && matchSearch;
    })
    .sort((a, b) => {
      if (sort === 'name')       return a.name.localeCompare(b.name);
      if (sort === 'status')     return effectiveStatus(a).localeCompare(effectiveStatus(b));
      if (sort === 'lastReport') return new Date(b.lastReportAt ?? 0).getTime() - new Date(a.lastReportAt ?? 0).getTime();
      if (sort === 'type')       return a.deviceType.localeCompare(b.deviceType);
      return 0;
    });

  return (
    <>
      <Navbar search={search} onSearch={setSearch} onRegister={() => setShowRegister(true)} />

      <main className="main-wrap">
        <SummaryStrip {...counts} active={filter} onFilter={f => setFilter(f as Filter)} />

        {/* Toolbar */}
        <div className="toolbar">
          <span className="toolbar-label">Filter</span>
          {(['all', 'online', 'degraded', 'offline', 'stale'] as Filter[]).map(f => (
            <div
              key={f}
              className={`filter-chip${filter === f ? ' active' : ''}`}
              onClick={() => setFilter(f)}
            >
              {f.charAt(0).toUpperCase() + f.slice(1)}
            </div>
          ))}

          <div className="toolbar-spacer" />

          <select className="sort-select" value={sort} onChange={e => setSort(e.target.value as SortKey)}>
            <option value="name">Sort: Name</option>
            <option value="status">Sort: Status</option>
            <option value="lastReport">Sort: Last Report</option>
            <option value="type">Sort: Type</option>
          </select>

          <div className="view-toggle">
            <button className={`view-btn${view === 'grid' ? ' active' : ''}`} onClick={() => setView('grid')} title="Grid view">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/>
                <rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/>
              </svg>
            </button>
            <button className={`view-btn${view === 'table' ? ' active' : ''}`} onClick={() => setView('table')} title="Table view">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M3 10h18M3 14h18M3 6h18M3 18h18"/>
              </svg>
            </button>
          </div>
        </div>

        {/* Grid view */}
        {view === 'grid' && (
          loading ? (
            <div className="empty-state">
              <div className="empty-title">Loading devices…</div>
            </div>
          ) : filtered.length === 0 ? (
            <div className="empty-state">
              <div className="empty-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/>
                  <rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/>
                </svg>
              </div>
              <div className="empty-title">No Devices Found</div>
              <div className="empty-sub">Try adjusting your filter or search term</div>
            </div>
          ) : (
            <div className="devices-grid">
              {filtered.map((d, i) => (
                <DeviceCard key={d.id} device={d} index={i} onClick={() => openDrawer(d.id)} />
              ))}
            </div>
          )
        )}

        {/* Table view */}
        <DeviceTable devices={filtered} visible={view === 'table'} onSelect={openDrawer} />
      </main>

      <footer className="footer">
        <span className="footer-text">NetWatch v1.0</span>
        <span className="footer-dot">·</span>
        <span className="footer-text">Network Device Monitoring</span>
        <span className="footer-dot">·</span>
        <span className="footer-text">BCS Group Assignment</span>
      </footer>

      <DetailDrawer
        open={drawerOpen}
        device={drawerDevice}
        loading={drawerLoading}
        onClose={closeDrawer}
        onSubmitReport={() => setShowReport(true)}
      />

      <RegisterModal
        open={showRegister}
        sites={sites}
        deviceTypes={deviceTypes}
        onClose={() => setShowRegister(false)}
        onSuccess={msg => { load(); showToast(msg); }}
      />

      {drawerDevice && (
        <SubmitReportModal
          open={showReport}
          deviceId={drawerDevice.id}
          deviceName={drawerDevice.name}
          onClose={() => setShowReport(false)}
          onSuccess={msg => { openDrawer(drawerDevice.id); showToast(msg); }}
        />
      )}

      <Toast
        visible={toast.visible}
        message={toast.message}
        type={toast.type}
        onHide={() => setToast(t => ({ ...t, visible: false }))}
      />
    </>
  );
}
