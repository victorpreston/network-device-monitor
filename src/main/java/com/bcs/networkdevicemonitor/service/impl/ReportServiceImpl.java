package com.bcs.networkdevicemonitor.service.impl;

import com.bcs.networkdevicemonitor.domain.entity.CurrentStatus;
import com.bcs.networkdevicemonitor.domain.entity.Device;
import com.bcs.networkdevicemonitor.domain.entity.Report;
import com.bcs.networkdevicemonitor.dto.request.SubmitReportRequest;
import com.bcs.networkdevicemonitor.exception.ResourceNotFoundException;
import com.bcs.networkdevicemonitor.repository.CurrentStatusRepository;
import com.bcs.networkdevicemonitor.repository.DeviceRepository;
import com.bcs.networkdevicemonitor.repository.ReportRepository;
import com.bcs.networkdevicemonitor.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportServiceImpl implements ReportService {

    private final DeviceRepository deviceRepository;
    private final ReportRepository reportRepository;
    private final CurrentStatusRepository currentStatusRepository;

    @Override
    public void submit(UUID deviceId, SubmitReportRequest request) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        OffsetDateTime now = OffsetDateTime.now();

        reportRepository.save(Report.builder()
                .device(device)
                .status(request.status())
                .message(request.message())
                .reportedAt(now)
                .build());

        CurrentStatus current = currentStatusRepository.findById(deviceId)
                .orElse(CurrentStatus.builder().device(device).build());

        current.setStatus(request.status());
        current.setMessage(request.message());
        current.setReportedAt(now);

        currentStatusRepository.save(current);
    }
}
