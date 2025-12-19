package com.usedcar.trading.domain.favorite.dto;

import com.usedcar.trading.domain.favorite.entity.Favorite;
import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.domain.vehicle.entity.VehicleStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class FavoriteResponse {

    private Long favoriteId;
    private LocalDateTime createdAt;

    private Long vehicleId;
    private String brand;
    private String model;
    private Integer modelYear;
    private BigDecimal price;
    private Integer mileage;
    private String thumbnailUrl;
    private VehicleStatus vehicleStatus;

    public static FavoriteResponse toResponse(Favorite favorite) {
        Vehicle vehicle = favorite.getVehicle();

        return FavoriteResponse.builder()
                .favoriteId(favorite.getFavoriteId())
                .createdAt(favorite.getCreatedAt())
                .vehicleId(vehicle.getVehicleId())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .modelYear(vehicle.getModelYear())
                .price(vehicle.getPrice())
                .mileage(vehicle.getMileage())
                .thumbnailUrl(vehicle.getThumbnailUrl())
                .vehicleStatus(vehicle.getVehicleStatus())
                .build();
    }
}