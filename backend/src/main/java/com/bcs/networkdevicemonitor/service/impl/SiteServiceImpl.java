package com.bcs.networkdevicemonitor.service.impl;

import com.bcs.networkdevicemonitor.domain.entity.Site;
import com.bcs.networkdevicemonitor.dto.request.RegisterSiteRequest;
import com.bcs.networkdevicemonitor.dto.response.SiteResponse;
import com.bcs.networkdevicemonitor.exception.ResourceNotFoundException;
import com.bcs.networkdevicemonitor.repository.SiteRepository;
import com.bcs.networkdevicemonitor.service.SiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SiteServiceImpl implements SiteService {

    private final SiteRepository siteRepository;

    @Override
    public SiteResponse register(RegisterSiteRequest request) {
        Site site = Site.builder()
                .name(request.name())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .createdAt(OffsetDateTime.now())
                .build();

        return toResponse(siteRepository.save(site));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteResponse> listAll() {
        return siteRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SiteResponse getById(UUID id) {
        return siteRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Site not found"));
    }

    private SiteResponse toResponse(Site site) {
        return new SiteResponse(site.getId(), site.getName(), site.getAddress(),
                site.getLatitude(), site.getLongitude(), site.getCreatedAt());
    }
}
