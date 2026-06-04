package com.bcs.networkdevicemonitor.service;

import com.bcs.networkdevicemonitor.dto.request.RegisterDeviceRequest;
import com.bcs.networkdevicemonitor.dto.response.DeviceDetailResponse;
import com.bcs.networkdevicemonitor.dto.response.DeviceListResponse;

import java.util.List;
import java.util.UUID;

public interface DeviceService {
    DeviceListResponse register(RegisterDeviceRequest request);
    List<DeviceListResponse> listAll();
    DeviceDetailResponse getById(UUID id);
}
