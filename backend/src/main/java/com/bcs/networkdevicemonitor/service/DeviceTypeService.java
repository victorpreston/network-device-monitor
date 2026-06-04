package com.bcs.networkdevicemonitor.service;

import com.bcs.networkdevicemonitor.dto.response.DeviceTypeResponse;

import java.util.List;

public interface DeviceTypeService {
    List<DeviceTypeResponse> listAll();
}
