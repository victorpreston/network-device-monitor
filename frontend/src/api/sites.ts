import client from './client';
import type { ApiResponse, SiteItem } from '../types';

export const getSites = () => client.get<ApiResponse<SiteItem[]>>('/sites');
