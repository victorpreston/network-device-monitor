package com.bcs.networkdevicemonitor.service.impl;

import com.bcs.networkdevicemonitor.domain.entity.*;
import com.bcs.networkdevicemonitor.domain.enums.DeviceStatus;
import com.bcs.networkdevicemonitor.dto.request.RegisterDeviceRequest;
import com.bcs.networkdevicemonitor.dto.response.DeviceDetailResponse;
import com.bcs.networkdevicemonitor.dto.response.DeviceListResponse;
import com.bcs.networkdevicemonitor.dto.response.ReportResponse;
import com.bcs.networkdevicemonitor.exception.ResourceNotFoundException;
import com.bcs.networkdevicemonitor.repository.*;
import com.bcs.networkdevicemonitor.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DeviceServiceImpl implements DeviceService {

    private static final int STALE_THRESHOLD_MINUTES = 15;

    private final DeviceRepository deviceRepository;
    private final DeviceTypeRepository deviceTypeRepository;
    private final SiteRepository siteRepository;
    private final CurrentStatusRepository currentStatusRepository;
    private final ReportRepository reportRepository;

    @Override
    public DeviceListResponse register(RegisterDeviceRequest request) {
        DeviceType type = deviceTypeRepository.findById(request.deviceTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Device type not found"));

        Site site = siteRepository.findById(request.siteId())
                .orElseThrow(() -> new ResourceNotFoundException("Site not found"));

        Device device = Device.builder()
                .name(request.name())
                .deviceType(type)
                .hostname(request.hostname())
                .site(site)
                .registeredAt(OffsetDateTime.now())
                .build();

        return toListResponse(deviceRepository.save(device), null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceListResponse> listAll(DeviceStatus status, Boolean stale) {
        List<Device> devices = deviceRepository.findAllWithDetails();
        List<DeviceListResponse> responses = toListResponses(devices);

        return responses.stream()
                .filter(r -> status == null || r.currentStatus() == status)
                .filter(r -> stale == null || r.stale() == stale)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceListResponse> listBySite(UUID siteId) {
        if (!siteRepository.existsById(siteId)) {
            throw new ResourceNotFoundException("Site not found");
        }
        return toListResponses(deviceRepository.findAllBySiteIdWithDetails(siteId));
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceDetailResponse getById(UUID id) {
        Device device = deviceRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        CurrentStatus cs = currentStatusRepository.findById(id).orElse(null);

        List<ReportResponse> reports = reportRepository
                .findTop20ByDeviceIdOrderByReportedAtDesc(id)
                .stream()
                .map(r -> new ReportResponse(r.getId(), r.getStatus(), r.getMessage(), r.getReportedAt()))
                .toList();

        return new DeviceDetailResponse(
                device.getId(),
                device.getName(),
                device.getDeviceType().getName(),
                device.getHostname(),
                device.getSite().getName(),
                device.getRegisteredAt(),
                cs != null ? cs.getStatus() : null,
                cs != null ? cs.getReportedAt() : null,
                isStale(cs),
                reports
        );
    }

    private List<DeviceListResponse> toListResponses(List<Device> devices) {
        Map<UUID, CurrentStatus> statusMap = currentStatusRepository
                .findAllById(devices.stream().map(Device::getId).toList())
                .stream()
                .collect(Collectors.toMap(cs -> cs.getDevice().getId(), cs -> cs));
        return devices.stream().map(d -> toListResponse(d, statusMap.get(d.getId()))).toList();
    }

    private boolean isStale(CurrentStatus cs) {
        if (cs == null) return true;
        return cs.getReportedAt().isBefore(OffsetDateTime.now().minusMinutes(STALE_THRESHOLD_MINUTES));
    }

    private DeviceListResponse toListResponse(Device device, CurrentStatus cs) {
        return new DeviceListResponse(
                device.getId(),
                device.getName(),
                device.getDeviceType().getName(),
                device.getHostname(),
                device.getSite().getName(),
                device.getRegisteredAt(),
                cs != null ? cs.getStatus() : null,
                cs != null ? cs.getReportedAt() : null,
                isStale(cs)
        );
    }
}
