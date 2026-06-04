package com.bcs.networkdevicemonitor.dto.response;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public record ApiMeta(String timestamp, String version, Integer count) {

    private static final String VERSION = "v1";

    public static ApiMeta of() {
        return new ApiMeta(now(), VERSION, null);
    }

    public static ApiMeta of(int count) {
        return new ApiMeta(now(), VERSION, count);
    }

    private static String now() {
        return OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
