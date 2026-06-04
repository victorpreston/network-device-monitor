import client from './client';
import type { ApiResponse, DeviceTypeItem } from '../types';

export const getDeviceTypes = () =>
  client.get<ApiResponse<DeviceTypeItem[]>>('/device-types');
