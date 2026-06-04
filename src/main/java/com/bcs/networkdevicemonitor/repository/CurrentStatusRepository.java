package com.bcs.networkdevicemonitor.repository;

import com.bcs.networkdevicemonitor.domain.entity.CurrentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CurrentStatusRepository extends JpaRepository<CurrentStatus, UUID> {
}
