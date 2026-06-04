package com.bcs.networkdevicemonitor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterDeviceRequest(
        @NotBlank(message = "Device name is required") String name,
        @NotNull(message = "Device type is required") UUID deviceTypeId,
        @NotBlank(message = "Hostname is required") String hostname,
        @NotNull(message = "Site is required") UUID siteId
) {}
