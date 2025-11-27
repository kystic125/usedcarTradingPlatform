package com.usedcar.trading.domain.vehicle.service;

import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.domain.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    @Transactional
    public void increaseViewCount(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("매물 없음"));

        vehicle.increaseViewCount();
    }
}
