package com.bcs.networkdevicemonitor.repository;

import com.bcs.networkdevicemonitor.domain.entity.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeviceTypeRepository extends JpaRepository<DeviceType, UUID> {
    Optional<DeviceType> findByName(String name);
}
