package com.bcs.networkdevicemonitor.service;

import com.bcs.networkdevicemonitor.dto.request.SubmitReportRequest;

import java.util.UUID;

public interface ReportService {
    void submit(UUID deviceId, SubmitReportRequest request);
}
