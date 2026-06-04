package com.bcs.networkdevicemonitor.repository;

import com.bcs.networkdevicemonitor.config.TestcontainersConfig;
import com.bcs.networkdevicemonitor.domain.entity.Device;
import com.bcs.networkdevicemonitor.domain.entity.DeviceType;
import com.bcs.networkdevicemonitor.domain.entity.Site;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfig.class)
@Transactional
class DeviceRepositoryTest {

    @Autowired DeviceRepository deviceRepository;
    @Autowired DeviceTypeRepository deviceTypeRepository;
    @Autowired SiteRepository siteRepository;

    private DeviceType router;
    private Site london;

    @BeforeEach
    void setUp() {
        router = deviceTypeRepository.findByName("Router").orElseThrow();
        london = siteRepository.save(Site.builder()
                .name("London-Test-" + System.nanoTime())
                .createdAt(OffsetDateTime.now())
                .build());
    }

    @Nested
    class FindAllWithDetails {

        @Test
        void returnsDevicesWithTypeAndSiteLoaded() {
            deviceRepository.save(device("Core Router"));

            List<Device> result = deviceRepository.findAllWithDetails();

            assertThat(result).isNotEmpty();
            assertThat(result).allSatisfy(d -> {
                assertThat(d.getDeviceType()).isNotNull();
                assertThat(d.getSite()).isNotNull();
            });
        }
    }

    @Nested
    class FindByIdWithDetails {

        @Test
        void returnsDevice_whenExists() {
            Device saved = deviceRepository.save(device("Switch-01"));

            Optional<Device> result = deviceRepository.findByIdWithDetails(saved.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getDeviceType().getName()).isEqualTo("Router");
            assertThat(result.get().getSite().getName()).isEqualTo(london.getName());
        }

        @Test
        void returnsEmpty_whenNotFound() {
            Optional<Device> result = deviceRepository.findByIdWithDetails(java.util.UUID.randomUUID());

            assertThat(result).isEmpty();
        }
    }

    private Device device(String name) {
        return Device.builder()
                .name(name)
                .deviceType(router)
                .hostname("192.168.1.1")
                .site(london)
                .registeredAt(OffsetDateTime.now())
                .build();
    }
}
