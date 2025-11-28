package com.usedcar.trading.domain.vehicle.scheduler;

import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.domain.vehicle.entity.VehicleStatus;
import com.usedcar.trading.domain.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleScheduler {

    private final VehicleRepository vehicleRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void checkExpiration() {
        log.info("스케줄러 실행: 만료된 매물 확인 중...");

        List<Vehicle> expiredVehicles = vehicleRepository.findByVehicleStatusAndExpirationDateBefore(
                VehicleStatus.SALE, LocalDateTime.now());

        if (expiredVehicles.isEmpty()) {
            log.info("만료된 매물이 없습니다.");
            return;
        }

        for (Vehicle vehicle : expiredVehicles) {
            vehicle.changeStatus(VehicleStatus.EXPIRED);
            log.info("매물 만료 처리: ID={}, 모델={}", vehicle.getVehicleId(), vehicle.getModel());

            // [VEH-014] 알림 발송
            // notificationService.send(vehicle.getRegisteredBy(), "사진 갱신이 필요하여 매물이 만료되었습니다.");
        }
    }
}