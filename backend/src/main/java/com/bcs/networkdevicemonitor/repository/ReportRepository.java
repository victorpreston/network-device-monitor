package com.bcs.networkdevicemonitor.repository;

import com.bcs.networkdevicemonitor.domain.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {
    List<Report> findTop20ByDeviceIdOrderByReportedAtDesc(UUID deviceId);
}
