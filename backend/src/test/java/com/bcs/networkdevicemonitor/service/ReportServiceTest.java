package com.bcs.networkdevicemonitor.service;

import com.bcs.networkdevicemonitor.domain.entity.*;
import com.bcs.networkdevicemonitor.domain.enums.DeviceStatus;
import com.bcs.networkdevicemonitor.dto.request.SubmitReportRequest;
import com.bcs.networkdevicemonitor.exception.ResourceNotFoundException;
import com.bcs.networkdevicemonitor.repository.CurrentStatusRepository;
import com.bcs.networkdevicemonitor.repository.DeviceRepository;
import com.bcs.networkdevicemonitor.repository.ReportRepository;
import com.bcs.networkdevicemonitor.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock DeviceRepository deviceRepository;
    @Mock ReportRepository reportRepository;
    @Mock CurrentStatusRepository currentStatusRepository;

    @InjectMocks ReportServiceImpl reportService;

    @Nested
    class Submit {

        @Test
        void createsReportAndCurrentStatus_whenFirstReport() {
            Device device = device();
            when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));
            when(currentStatusRepository.findById(device.getId())).thenReturn(Optional.empty());
            when(reportRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(currentStatusRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            reportService.submit(device.getId(), new SubmitReportRequest(DeviceStatus.ONLINE, "All interfaces up"));

            verify(reportRepository).save(any());
            ArgumentCaptor<CurrentStatus> captor = ArgumentCaptor.forClass(CurrentStatus.class);
            verify(currentStatusRepository).save(captor.capture());

            assertThat(captor.getValue().getStatus()).isEqualTo(DeviceStatus.ONLINE);
            assertThat(captor.getValue().getMessage()).isEqualTo("All interfaces up");
        }

        @Test
        void updatesExistingCurrentStatus_onSubsequentReport() {
            Device device = device();
            CurrentStatus existing = CurrentStatus.builder()
                    .device(device)
                    .status(DeviceStatus.ONLINE)
                    .reportedAt(OffsetDateTime.now().minusMinutes(10))
                    .build();

            when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));
            when(currentStatusRepository.findById(device.getId())).thenReturn(Optional.of(existing));
            when(reportRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(currentStatusRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            reportService.submit(device.getId(), new SubmitReportRequest(DeviceStatus.DEGRADED, "High latency"));

            ArgumentCaptor<CurrentStatus> captor = ArgumentCaptor.forClass(CurrentStatus.class);
            verify(currentStatusRepository).save(captor.capture());

            assertThat(captor.getValue().getStatus()).isEqualTo(DeviceStatus.DEGRADED);
            assertThat(captor.getValue().getMessage()).isEqualTo("High latency");
        }

        @Test
        void throws_whenDeviceNotFound() {
            UUID id = UUID.randomUUID();
            when(deviceRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reportService.submit(id, new SubmitReportRequest(DeviceStatus.ONLINE, null)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Device not found");

            verifyNoInteractions(reportRepository, currentStatusRepository);
        }
    }

    private Device device() {
        DeviceType type = DeviceType.builder().id(UUID.randomUUID()).name("Router").build();
        Site site = Site.builder().id(UUID.randomUUID()).name("London-01").createdAt(OffsetDateTime.now()).build();
        return Device.builder()
                .id(UUID.randomUUID())
                .name("Core Router")
                .deviceType(type)
                .hostname("192.168.1.1")
                .site(site)
                .registeredAt(OffsetDateTime.now())
                .build();
    }
}
