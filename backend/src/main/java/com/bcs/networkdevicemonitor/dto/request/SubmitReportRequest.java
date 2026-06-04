package com.bcs.networkdevicemonitor.dto.request;

import com.bcs.networkdevicemonitor.domain.enums.DeviceStatus;
import jakarta.validation.constraints.NotNull;

public record SubmitReportRequest(
        @NotNull(message = "Status is required") DeviceStatus status,
        String message
) {}
