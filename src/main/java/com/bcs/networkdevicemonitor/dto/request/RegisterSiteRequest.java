package com.bcs.networkdevicemonitor.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RegisterSiteRequest(
        @NotBlank(message = "Site name is required") String name,
        String address
) {}
