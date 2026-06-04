package com.bcs.networkdevicemonitor.repository;

import com.bcs.networkdevicemonitor.domain.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    @Query("SELECT d FROM Device d JOIN FETCH d.deviceType JOIN FETCH d.site")
    List<Device> findAllWithDetails();

    @Query("SELECT d FROM Device d JOIN FETCH d.deviceType JOIN FETCH d.site WHERE d.id = :id")
    Optional<Device> findByIdWithDetails(UUID id);

    @Query("SELECT d FROM Device d JOIN FETCH d.deviceType JOIN FETCH d.site WHERE d.site.id = :siteId")
    List<Device> findAllBySiteIdWithDetails(UUID siteId);
}
