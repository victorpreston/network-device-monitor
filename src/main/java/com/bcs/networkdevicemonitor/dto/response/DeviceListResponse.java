package com.bcs.networkdevicemonitor.dto.response;

import com.bcs.networkdevicemonitor.domain.enums.DeviceStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeviceListResponse(
        UUID id,
        String name,
        String deviceType,
        String hostname,
        String site,
        OffsetDateTime registeredAt,
        DeviceStatus currentStatus,
        OffsetDateTime lastReportAt,
        boolean stale
) {}
