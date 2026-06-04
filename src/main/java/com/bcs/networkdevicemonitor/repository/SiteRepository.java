package com.bcs.networkdevicemonitor.repository;

import com.bcs.networkdevicemonitor.domain.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SiteRepository extends JpaRepository<Site, UUID> {
    Optional<Site> findByName(String name);
}
