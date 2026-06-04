import client from './client';
import type { ApiResponse, DeviceDetail, DeviceListItem, DeviceStatus } from '../types';

export const getDevices = (status?: DeviceStatus, stale?: boolean) => {
  const params: Record<string, string> = {};
  if (status) params.status = status;
  if (stale !== undefined) params.stale = String(stale);
  return client.get<ApiResponse<DeviceListItem[]>>('/devices', { params });
};

export const getDevice = (id: string) =>
  client.get<ApiResponse<DeviceDetail>>(`/devices/${id}`);

export const submitReport = (id: string, status: DeviceStatus, message: string) =>
  client.post<ApiResponse<null>>(`/devices/${id}/reports`, { status, message });

export const registerDevice = (data: {
  name: string;
  deviceTypeId: string;
  hostname: string;
  siteId: string;
}) => client.post<ApiResponse<DeviceListItem>>('/devices', data);
