export type DeviceStatus = 'ONLINE' | 'DEGRADED' | 'OFFLINE';

export interface DeviceListItem {
  id: string;
  name: string;
  deviceType: string;
  hostname: string;
  site: string;
  registeredAt: string;
  currentStatus: DeviceStatus | null;
  lastReportAt: string | null;
  stale: boolean;
}

export interface ReportItem {
  id: string;
  status: DeviceStatus;
  message: string | null;
  reportedAt: string;
}

export interface DeviceDetail extends DeviceListItem {
  recentReports: ReportItem[];
}

export interface SiteItem {
  id: string;
  name: string;
  address: string | null;
  latitude: number | null;
  longitude: number | null;
  createdAt: string;
}

export interface DeviceTypeItem {
  id: string;
  name: string;
}

export interface ApiError {
  message: string;
  code: string;
  field: string | null;
}

export interface ApiResponse<T> {
  data: T | null;
  errors: ApiError[] | null;
  meta: { timestamp: string; version: string; count?: number };
}
