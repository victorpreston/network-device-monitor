package com.bcs.networkdevicemonitor.controller;

import com.bcs.networkdevicemonitor.domain.enums.DeviceStatus;
import com.bcs.networkdevicemonitor.dto.request.RegisterDeviceRequest;
import com.bcs.networkdevicemonitor.dto.request.SubmitReportRequest;
import com.bcs.networkdevicemonitor.dto.response.ApiResponse;
import com.bcs.networkdevicemonitor.dto.response.DeviceDetailResponse;
import com.bcs.networkdevicemonitor.dto.response.DeviceListResponse;
import com.bcs.networkdevicemonitor.service.DeviceService;
import com.bcs.networkdevicemonitor.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final ReportService reportService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DeviceListResponse> register(@Valid @RequestBody RegisterDeviceRequest request) {
        return ApiResponse.success(deviceService.register(request));
    }

    @GetMapping
    public ApiResponse<List<DeviceListResponse>> listAll(
            @RequestParam(required = false) DeviceStatus status,
            @RequestParam(required = false) Boolean stale) {
        List<DeviceListResponse> devices = deviceService.listAll(status, stale);
        return ApiResponse.success(devices, devices.size());
    }

    @GetMapping("/{id}")
    public ApiResponse<DeviceDetailResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(deviceService.getById(id));
    }

    @PostMapping("/{id}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> submitReport(@PathVariable UUID id, @Valid @RequestBody SubmitReportRequest request) {
        reportService.submit(id, request);
        return ApiResponse.success(null);
    }
}
