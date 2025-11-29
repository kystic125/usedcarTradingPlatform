package com.usedcar.trading.domain.vehicle.scheduler;

import com.usedcar.trading.domain.notification.entity.NotificationType;
import com.usedcar.trading.domain.notification.service.NotificationService;
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
    private final NotificationService notificationService;

    /**
     * 매일 자정에 만료된 매물 처리 [VEH-012]
     */
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
            notificationService.createNotification(
                    vehicle.getRegisteredBy().getUser(),
                    NotificationType.VEHICLE_EXPIRED,
                    String.format("[%s %s] 매물이 만료되었습니다. 사진 갱신이 필요합니다.", vehicle.getBrand(), vehicle.getModel()),
                    "/vehicles/" + vehicle.getVehicleId()
            );
        }

        log.info("만료 처리 완료: {}건", expiredVehicles.size());
    }

    /**
     * 매일 오전 9시에 만료 임박 매물 알림 [VEH-012]
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional(readOnly = true)
    public void notifyExpiringVehicles() {
        log.info("스케줄러 실행: 만료 임박 매물 알림 중...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysLater = now.plusDays(3);

        List<Vehicle> expiringVehicles = vehicleRepository.findByVehicleStatusAndExpirationDateBetween(
                VehicleStatus.SALE, now, threeDaysLater);

        if (expiringVehicles.isEmpty()) {
            log.info("만료 임박 매물이 없습니다.");
            return;
        }

        for (Vehicle vehicle : expiringVehicles) {
            notificationService.createNotification(
                    vehicle.getRegisteredBy().getUser(),
                    NotificationType.VEHICLE_EXPIRING,
                    String.format("[%s %s] 매물이 3일 이내에 만료됩니다. 사진을 갱신해주세요.", vehicle.getBrand(), vehicle.getModel()),
                    "/vehicles/" + vehicle.getVehicleId()
            );
        }

        log.info("만료 임박 알림 완료: {}건", expiringVehicles.size());
    }
}