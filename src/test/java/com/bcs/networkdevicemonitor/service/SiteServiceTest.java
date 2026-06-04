package com.bcs.networkdevicemonitor.service;

import com.bcs.networkdevicemonitor.domain.entity.Site;
import com.bcs.networkdevicemonitor.dto.request.RegisterSiteRequest;
import com.bcs.networkdevicemonitor.dto.response.SiteResponse;
import com.bcs.networkdevicemonitor.repository.SiteRepository;
import com.bcs.networkdevicemonitor.service.impl.SiteServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SiteServiceTest {

    @Mock SiteRepository siteRepository;

    @InjectMocks SiteServiceImpl siteService;

    @Test
    void register_returnsSiteResponse_whenRequestIsValid() {
        Site saved = Site.builder()
                .id(UUID.randomUUID())
                .name("London-01")
                .address("1 Tech Street, London")
                .createdAt(OffsetDateTime.now())
                .build();

        when(siteRepository.save(any())).thenReturn(saved);

        SiteResponse response = siteService.register(new RegisterSiteRequest("London-01", "1 Tech Street, London"));

        assertThat(response.name()).isEqualTo("London-01");
        assertThat(response.address()).isEqualTo("1 Tech Street, London");
        assertThat(response.id()).isNotNull();
    }

    @Test
    void listAll_returnsAllSites() {
        List<Site> sites = List.of(
                Site.builder().id(UUID.randomUUID()).name("London-01").createdAt(OffsetDateTime.now()).build(),
                Site.builder().id(UUID.randomUUID()).name("Manchester-01").createdAt(OffsetDateTime.now()).build()
        );

        when(siteRepository.findAll()).thenReturn(sites);

        List<SiteResponse> result = siteService.listAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(SiteResponse::name)
                .containsExactly("London-01", "Manchester-01");
    }

    @Test
    void listAll_returnsEmptyList_whenNoSitesExist() {
        when(siteRepository.findAll()).thenReturn(List.of());

        assertThat(siteService.listAll()).isEmpty();
    }
}
