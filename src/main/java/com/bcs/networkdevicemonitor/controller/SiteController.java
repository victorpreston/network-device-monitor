package com.bcs.networkdevicemonitor.controller;

import com.bcs.networkdevicemonitor.dto.request.RegisterSiteRequest;
import com.bcs.networkdevicemonitor.dto.response.ApiResponse;
import com.bcs.networkdevicemonitor.dto.response.SiteResponse;
import com.bcs.networkdevicemonitor.service.SiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SiteResponse> register(@Valid @RequestBody RegisterSiteRequest request) {
        return ApiResponse.success(siteService.register(request));
    }

    @GetMapping
    public ApiResponse<List<SiteResponse>> listAll() {
        List<SiteResponse> sites = siteService.listAll();
        return ApiResponse.success(sites, sites.size());
    }
}
