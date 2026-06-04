package com.bcs.networkdevicemonitor.service;

import com.bcs.networkdevicemonitor.domain.entity.*;
import com.bcs.networkdevicemonitor.domain.enums.DeviceStatus;
import com.bcs.networkdevicemonitor.dto.request.RegisterDeviceRequest;
import com.bcs.networkdevicemonitor.dto.response.DeviceDetailResponse;
import com.bcs.networkdevicemonitor.dto.response.DeviceListResponse;
import com.bcs.networkdevicemonitor.exception.ResourceNotFoundException;
import com.bcs.networkdevicemonitor.repository.*;
import com.bcs.networkdevicemonitor.service.impl.DeviceServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock DeviceRepository deviceRepository;
    @Mock DeviceTypeRepository deviceTypeRepository;
    @Mock SiteRepository siteRepository;
    @Mock CurrentStatusRepository currentStatusRepository;
    @Mock ReportRepository reportRepository;

    @InjectMocks DeviceServiceImpl deviceService;

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_returnsResponse_whenRequestIsValid() {
        UUID typeId = UUID.randomUUID();
        UUID siteId = UUID.randomUUID();
        DeviceType type = DeviceType.builder().id(typeId).name("Router").build();
        Site site = Site.builder().id(siteId).name("London-01").createdAt(OffsetDateTime.now()).build();
        Device saved = device(type, site);

        when(deviceTypeRepository.findById(typeId)).thenReturn(Optional.of(type));
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));
        when(deviceRepository.save(any())).thenReturn(saved);

        DeviceListResponse response = deviceService.register(new RegisterDeviceRequest("Core Router", typeId, "192.168.1.1", siteId));

        assertThat(response.name()).isEqualTo("Core Router");
        assertThat(response.deviceType()).isEqualTo("Router");
        assertThat(response.currentStatus()).isNull();
        assertThat(response.stale()).isTrue();
    }

    @Test
    void register_throws_whenDeviceTypeNotFound() {
        UUID typeId = UUID.randomUUID();
        when(deviceTypeRepository.findById(typeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.register(new RegisterDeviceRequest("R1", typeId, "10.0.0.1", UUID.randomUUID())))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Device type not found");
    }

    @Test
    void register_throws_whenSiteNotFound() {
        UUID typeId = UUID.randomUUID();
        UUID siteId = UUID.randomUUID();
        when(deviceTypeRepository.findById(typeId)).thenReturn(Optional.of(DeviceType.builder().id(typeId).name("Router").build()));
        when(siteRepository.findById(siteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.register(new RegisterDeviceRequest("R1", typeId, "10.0.0.1", siteId)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Site not found");
    }

    // ── listAll / stale detection ─────────────────────────────────────────────

    @Test
    void listAll_marksStale_whenDeviceHasNeverReported() {
        Device d = device();
        when(deviceRepository.findAllWithDetails()).thenReturn(List.of(d));
        when(currentStatusRepository.findAllById(any())).thenReturn(List.of());

        List<DeviceListResponse> result = deviceService.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).stale()).isTrue();
        assertThat(result.get(0).currentStatus()).isNull();
    }

    @Test
    void listAll_marksStale_whenLastReportIsOlderThan15Minutes() {
        Device d = device();
        CurrentStatus cs = currentStatus(d, DeviceStatus.ONLINE, OffsetDateTime.now().minusMinutes(20));

        when(deviceRepository.findAllWithDetails()).thenReturn(List.of(d));
        when(currentStatusRepository.findAllById(any())).thenReturn(List.of(cs));

        List<DeviceListResponse> result = deviceService.listAll();

        assertThat(result.get(0).stale()).isTrue();
    }

    @Test
    void listAll_notStale_whenLastReportIsWithin15Minutes() {
        Device d = device();
        CurrentStatus cs = currentStatus(d, DeviceStatus.ONLINE, OffsetDateTime.now().minusMinutes(5));

        when(deviceRepository.findAllWithDetails()).thenReturn(List.of(d));
        when(currentStatusRepository.findAllById(any())).thenReturn(List.of(cs));

        List<DeviceListResponse> result = deviceService.listAll();

        assertThat(result.get(0).stale()).isFalse();
        assertThat(result.get(0).currentStatus()).isEqualTo(DeviceStatus.ONLINE);
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    void getById_returnsDetailWithReports_whenDeviceExists() {
        Device d = device();
        when(deviceRepository.findByIdWithDetails(d.getId())).thenReturn(Optional.of(d));
        when(currentStatusRepository.findById(d.getId())).thenReturn(Optional.empty());
        when(reportRepository.findTop20ByDeviceIdOrderByReportedAtDesc(d.getId())).thenReturn(List.of());

        DeviceDetailResponse response = deviceService.getById(d.getId());

        assertThat(response.id()).isEqualTo(d.getId());
        assertThat(response.recentReports()).isEmpty();
        assertThat(response.stale()).isTrue();
    }

    @Test
    void getById_throws_whenDeviceNotFound() {
        UUID id = UUID.randomUUID();
        when(deviceRepository.findByIdWithDetails(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Device not found");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Device device() {
        DeviceType type = DeviceType.builder().id(UUID.randomUUID()).name("Router").build();
        Site site = Site.builder().id(UUID.randomUUID()).name("London-01").createdAt(OffsetDateTime.now()).build();
        return device(type, site);
    }

    private Device device(DeviceType type, Site site) {
        return Device.builder()
                .id(UUID.randomUUID())
                .name("Core Router")
                .deviceType(type)
                .hostname("192.168.1.1")
                .site(site)
                .registeredAt(OffsetDateTime.now())
                .build();
    }

    private CurrentStatus currentStatus(Device device, DeviceStatus status, OffsetDateTime reportedAt) {
        return CurrentStatus.builder()
                .device(device)
                .status(status)
                .reportedAt(reportedAt)
                .build();
    }
}
