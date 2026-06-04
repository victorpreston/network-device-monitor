package com.bcs.networkdevicemonitor.controller;

import com.bcs.networkdevicemonitor.dto.response.ApiResponse;
import com.bcs.networkdevicemonitor.dto.response.DeviceTypeResponse;
import com.bcs.networkdevicemonitor.service.DeviceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/device-types")
@RequiredArgsConstructor
public class DeviceTypeController {

    private final DeviceTypeService deviceTypeService;

    @GetMapping
    public ApiResponse<List<DeviceTypeResponse>> listAll() {
        List<DeviceTypeResponse> types = deviceTypeService.listAll();
        return ApiResponse.success(types, types.size());
    }
}
