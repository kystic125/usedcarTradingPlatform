package com.usedcar.trading.domain.vehicle.service;

import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.domain.vehicle.entity.VehicleStatus;
import com.usedcar.trading.domain.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Transactional
    public void renewVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("매물 없음"));

        if (vehicle.getVehicleStatus() != VehicleStatus.EXPIRED) {
            throw new IllegalStateException("갱신 가능한 상태가 아닙니다.");
        }

        vehicle.extendExpirationDate();

        vehicle.changeStatus(VehicleStatus.PENDING);
    }
}
