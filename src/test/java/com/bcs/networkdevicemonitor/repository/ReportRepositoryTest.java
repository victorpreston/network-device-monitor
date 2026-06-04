package com.bcs.networkdevicemonitor.repository;

import com.bcs.networkdevicemonitor.config.TestcontainersConfig;
import com.bcs.networkdevicemonitor.domain.entity.Device;
import com.bcs.networkdevicemonitor.domain.entity.DeviceType;
import com.bcs.networkdevicemonitor.domain.entity.Report;
import com.bcs.networkdevicemonitor.domain.entity.Site;
import com.bcs.networkdevicemonitor.domain.enums.DeviceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfig.class)
@Transactional
class ReportRepositoryTest {

    @Autowired ReportRepository reportRepository;
    @Autowired DeviceRepository deviceRepository;
    @Autowired DeviceTypeRepository deviceTypeRepository;
    @Autowired SiteRepository siteRepository;

    private Device device;

    @BeforeEach
    void setUp() {
        DeviceType type = deviceTypeRepository.findByName("Router").orElseThrow();
        Site site = siteRepository.save(Site.builder()
                .name("London-Report-" + System.nanoTime())
                .createdAt(OffsetDateTime.now())
                .build());
        device = deviceRepository.save(Device.builder()
                .name("Test Device")
                .deviceType(type)
                .hostname("10.0.0.1")
                .site(site)
                .registeredAt(OffsetDateTime.now())
                .build());
    }

    @Nested
    class FindTop20 {

        @Test
        void returnsAtMost20Reports_orderedByTimeDescending() {
            IntStream.rangeClosed(1, 25).forEach(i ->
                    reportRepository.save(Report.builder()
                            .device(device)
                            .status(DeviceStatus.ONLINE)
                            .reportedAt(OffsetDateTime.now().minusMinutes(i))
                            .build()));

            List<Report> result = reportRepository.findTop20ByDeviceIdOrderByReportedAtDesc(device.getId());

            assertThat(result).hasSize(20);
            assertThat(result).isSortedAccordingTo(
                    (a, b) -> b.getReportedAt().compareTo(a.getReportedAt()));
        }

        @Test
        void returnsOnlyReportsForSpecifiedDevice() {
            DeviceType type = deviceTypeRepository.findByName("Switch").orElseThrow();
            Site site = siteRepository.save(Site.builder()
                    .name("Other-Site-" + System.nanoTime())
                    .createdAt(OffsetDateTime.now())
                    .build());
            Device other = deviceRepository.save(Device.builder()
                    .name("Other Device")
                    .deviceType(type)
                    .hostname("10.0.0.2")
                    .site(site)
                    .registeredAt(OffsetDateTime.now())
                    .build());

            reportRepository.save(Report.builder().device(device).status(DeviceStatus.ONLINE).reportedAt(OffsetDateTime.now()).build());
            reportRepository.save(Report.builder().device(other).status(DeviceStatus.OFFLINE).reportedAt(OffsetDateTime.now()).build());

            List<Report> result = reportRepository.findTop20ByDeviceIdOrderByReportedAtDesc(device.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDevice().getId()).isEqualTo(device.getId());
        }
    }
}
