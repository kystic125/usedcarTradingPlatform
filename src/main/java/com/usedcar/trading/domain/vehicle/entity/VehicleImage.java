package com.usedcar.trading.domain.vehicle.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class VehicleImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    private String imageUrl;

    private String thumbnailUrl;

    private int displayOrder;

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }
}
