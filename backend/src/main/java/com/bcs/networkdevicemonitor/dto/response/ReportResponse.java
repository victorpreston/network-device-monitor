package com.bcs.networkdevicemonitor.dto.response;

import com.bcs.networkdevicemonitor.domain.enums.DeviceStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        DeviceStatus status,
        String message,
        OffsetDateTime reportedAt
) {}
