package com.bcs.networkdevicemonitor.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SiteResponse(
        UUID id,
        String name,
        String address,
        OffsetDateTime createdAt
) {}
