package com.bcs.networkdevicemonitor.domain.entity;

import com.bcs.networkdevicemonitor.domain.enums.DeviceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "current_status")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentStatus {

    @Id
    private UUID deviceId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "device_id")
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceStatus status;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private OffsetDateTime reportedAt;
}
