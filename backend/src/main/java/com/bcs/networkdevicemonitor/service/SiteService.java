package com.bcs.networkdevicemonitor.service;

import com.bcs.networkdevicemonitor.dto.request.RegisterSiteRequest;
import com.bcs.networkdevicemonitor.dto.response.SiteResponse;

import java.util.List;
import java.util.UUID;

public interface SiteService {
    SiteResponse register(RegisterSiteRequest request);
    List<SiteResponse> listAll();
    SiteResponse getById(UUID id);
}
