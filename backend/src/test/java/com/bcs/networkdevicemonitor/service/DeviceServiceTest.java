package com.bcs.networkdevicemonitor.service;

import com.bcs.networkdevicemonitor.domain.entity.*;
import com.bcs.networkdevicemonitor.domain.enums.DeviceStatus;
import com.bcs.networkdevicemonitor.dto.request.RegisterDeviceRequest;
import com.bcs.networkdevicemonitor.dto.response.DeviceDetailResponse;
import com.bcs.networkdevicemonitor.dto.response.DeviceListResponse;
import com.bcs.networkdevicemonitor.exception.ResourceNotFoundException;
import com.bcs.networkdevicemonitor.repository.*;
import com.bcs.networkdevicemonitor.service.impl.DeviceServiceImpl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @Nested
    class Register {

        @Test
        void returnsResponse_whenRequestIsValid() {
            UUID typeId = UUID.randomUUID();
            UUID siteId = UUID.randomUUID();
            DeviceType type = type(typeId, "Router");
            Site site = site(siteId, "London-01");
            Device saved = device(type, site);

            when(deviceTypeRepository.findById(typeId)).thenReturn(Optional.of(type));
            when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));
            when(deviceRepository.save(any())).thenReturn(saved);

            DeviceListResponse response = deviceService.register(
                    new RegisterDeviceRequest("Core Router", typeId, "192.168.1.1", siteId));

            assertThat(response.name()).isEqualTo("Core Router");
            assertThat(response.deviceType()).isEqualTo("Router");
            assertThat(response.currentStatus()).isNull();
            assertThat(response.stale()).isTrue();
        }

        @Test
        void throws_whenDeviceTypeNotFound() {
            UUID typeId = UUID.randomUUID();
            when(deviceTypeRepository.findById(typeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deviceService.register(
                    new RegisterDeviceRequest("R1", typeId, "10.0.0.1", UUID.randomUUID())))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Device type not found");
        }

        @Test
        void throws_whenSiteNotFound() {
            UUID typeId = UUID.randomUUID();
            UUID siteId = UUID.randomUUID();
            when(deviceTypeRepository.findById(typeId)).thenReturn(Optional.of(type(typeId, "Router")));
            when(siteRepository.findById(siteId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deviceService.register(
                    new RegisterDeviceRequest("R1", typeId, "10.0.0.1", siteId)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Site not found");
        }
    }

    @Nested
    class ListAll {

        @Test
        void marksStale_whenDeviceHasNeverReported() {
            Device d = device();
            when(deviceRepository.findAllWithDetails()).thenReturn(List.of(d));
            when(currentStatusRepository.findAllById(any())).thenReturn(List.of());

            List<DeviceListResponse> result = deviceService.listAll(null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).stale()).isTrue();
            assertThat(result.get(0).currentStatus()).isNull();
        }

        @Test
        void marksStale_whenLastReportOlderThan15Minutes() {
            Device d = device();
            CurrentStatus cs = currentStatus(d, DeviceStatus.ONLINE, OffsetDateTime.now().minusMinutes(20));

            when(deviceRepository.findAllWithDetails()).thenReturn(List.of(d));
            when(currentStatusRepository.findAllById(any())).thenReturn(List.of(cs));

            assertThat(deviceService.listAll(null, null).get(0).stale()).isTrue();
        }

        @Test
        void notStale_whenLastReportWithin15Minutes() {
            Device d = device();
            CurrentStatus cs = currentStatus(d, DeviceStatus.ONLINE, OffsetDateTime.now().minusMinutes(5));

            when(deviceRepository.findAllWithDetails()).thenReturn(List.of(d));
            when(currentStatusRepository.findAllById(any())).thenReturn(List.of(cs));

            DeviceListResponse result = deviceService.listAll(null, null).get(0);

            assertThat(result.stale()).isFalse();
            assertThat(result.currentStatus()).isEqualTo(DeviceStatus.ONLINE);
        }

        @Test
        void filtersbyStatus_whenStatusParamProvided() {
            Device online = device();
            Device offline = device();
            CurrentStatus onlineCs = currentStatus(online, DeviceStatus.ONLINE, OffsetDateTime.now().minusMinutes(1));
            CurrentStatus offlineCs = currentStatus(offline, DeviceStatus.OFFLINE, OffsetDateTime.now().minusMinutes(1));

            when(deviceRepository.findAllWithDetails()).thenReturn(List.of(online, offline));
            when(currentStatusRepository.findAllById(any())).thenReturn(List.of(onlineCs, offlineCs));

            List<DeviceListResponse> result = deviceService.listAll(DeviceStatus.OFFLINE, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).currentStatus()).isEqualTo(DeviceStatus.OFFLINE);
        }

        @Test
        void filtersStale_whenStaleParamIsTrue() {
            Device fresh = device();
            Device stale = device();
            CurrentStatus freshCs = currentStatus(fresh, DeviceStatus.ONLINE, OffsetDateTime.now().minusMinutes(1));

            when(deviceRepository.findAllWithDetails()).thenReturn(List.of(fresh, stale));
            when(currentStatusRepository.findAllById(any())).thenReturn(List.of(freshCs));

            List<DeviceListResponse> result = deviceService.listAll(null, true);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).stale()).isTrue();
        }
    }

    @Nested
    class ListBySite {

        @Test
        void returnsDevicesForSite() {
            UUID siteId = UUID.randomUUID();
            Site site = site(siteId, "London-01");
            Device d = device(type(UUID.randomUUID(), "Router"), site);

            when(siteRepository.existsById(siteId)).thenReturn(true);
            when(deviceRepository.findAllBySiteIdWithDetails(siteId)).thenReturn(List.of(d));
            when(currentStatusRepository.findAllById(any())).thenReturn(List.of());

            List<DeviceListResponse> result = deviceService.listBySite(siteId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).site()).isEqualTo("London-01");
        }

        @Test
        void returnsEmptyList_whenSiteHasNoDevices() {
            UUID siteId = UUID.randomUUID();

            when(siteRepository.existsById(siteId)).thenReturn(true);
            when(deviceRepository.findAllBySiteIdWithDetails(siteId)).thenReturn(List.of());
            when(currentStatusRepository.findAllById(any())).thenReturn(List.of());

            assertThat(deviceService.listBySite(siteId)).isEmpty();
        }

        @Test
        void throws_whenSiteNotFound() {
            UUID siteId = UUID.randomUUID();
            when(siteRepository.existsById(siteId)).thenReturn(false);

            assertThatThrownBy(() -> deviceService.listBySite(siteId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Site not found");
        }
    }

    @Nested
    class GetById {

        @Test
        void returnsDetailWithReports_whenDeviceExists() {
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
        void throws_whenDeviceNotFound() {
            UUID id = UUID.randomUUID();
            when(deviceRepository.findByIdWithDetails(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deviceService.getById(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Device not found");
        }
    }

    private Device device() {
        return device(type(UUID.randomUUID(), "Router"), site(UUID.randomUUID(), "London-01"));
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

    private DeviceType type(UUID id, String name) {
        return DeviceType.builder().id(id).name(name).build();
    }

    private Site site(UUID id, String name) {
        return Site.builder().id(id).name(name).createdAt(OffsetDateTime.now()).build();
    }

    private CurrentStatus currentStatus(Device device, DeviceStatus status, OffsetDateTime reportedAt) {
        return CurrentStatus.builder().device(device).status(status).reportedAt(reportedAt).build();
    }
}
