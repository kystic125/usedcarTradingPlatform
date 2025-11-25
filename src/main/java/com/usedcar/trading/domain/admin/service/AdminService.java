package com.usedcar.trading.domain.admin.service;

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
public class AdminService {

    private final VehicleRepository vehicleRepository;

    // 1. 승인 대기중인 매물 목록 가져오기
    @Transactional(readOnly = true)
    public List<Vehicle> getPendingVehicles() {
        return vehicleRepository.findByVehicleStatus(VehicleStatus.PENDING);
    }

    // 2. 매물 승인 처리
    @Transactional
    public void approveVehicle(Long vehicleId, User admin) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("매물을 찾을 수 없습니다."));

        vehicle.approve(admin);
    }
}