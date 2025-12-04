package com.usedcar.trading.domain.vehicle.dto;

import com.usedcar.trading.domain.vehicle.entity.FuelType;
import com.usedcar.trading.domain.vehicle.entity.Transmission;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@ToString
public class VehicleRegisterRequest {
    private String brand;
    private String model;
    private int modelYear;
    private int mileage;
    private BigDecimal price;
    private FuelType fuelType;
    private Transmission transmission;
    private String color;
    private Boolean accidentHistory;
    private List<String> options;
    private String description;
}