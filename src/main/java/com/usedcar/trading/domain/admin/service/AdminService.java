package com.usedcar.trading.domain.admin.service;

import com.usedcar.trading.domain.company.repository.CompanyRepository;
import com.usedcar.trading.domain.notification.entity.NotificationType;
import com.usedcar.trading.domain.notification.service.NotificationService;
import com.usedcar.trading.domain.report.entity.ReportStatus;
import com.usedcar.trading.domain.report.repository.ReportRepository;
import com.usedcar.trading.domain.settlement.entity.SettlementStatus;
import com.usedcar.trading.domain.settlement.repository.SettlementRepository;
import com.usedcar.trading.domain.transaction.entity.TransactionStatus;
import com.usedcar.trading.domain.transaction.repository.TransactionRepository;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.domain.vehicle.entity.VehicleStatus;
import com.usedcar.trading.domain.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final TransactionRepository transactionRepository;
    private final SettlementRepository settlementRepository;
    private final ReportRepository reportRepository;
    private final NotificationService notificationService;

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

        vehicle.extendExpirationDate();

        notificationService.createNotification(
                vehicle.getRegisteredBy().getUser(),
                NotificationType.VEHICLE_APPROVED,
                String.format("'%s' 매물 등록이 승인되었습니다.", vehicle.getModel()),
                "/vehicles/" + vehicle.getVehicleId()
        );
    }

    // 3. 매물 반려 처리
    @Transactional
    public void rejectVehicle(Long vehicleId, String reason, User admin) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("매물을 찾을 수 없습니다."));

        if (vehicle.getVehicleStatus() != VehicleStatus.PENDING) {
            throw new IllegalStateException("대기 중인 매물만 반려할 수 있습니다.");
        }

        vehicle.reject(reason, admin);

        notificationService.createNotification(
                vehicle.getRegisteredBy().getUser(),
                NotificationType.VEHICLE_REJECTED,
                String.format("'%s' 매물이 반려되었습니다. 사유: %s", vehicle.getModel(), reason),
                "/company/sales"
        );
    }

    /**
     * 대시보드 통계 조회 [ADM-005]
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 사용자 통계
        stats.put("totalUsers", userRepository.count());

        // 업체 통계
        stats.put("totalCompanies", companyRepository.count());

        // 매물 통계
        stats.put("pendingVehicles", vehicleRepository.countByVehicleStatus(VehicleStatus.PENDING));
        stats.put("saleVehicles", vehicleRepository.countByVehicleStatus(VehicleStatus.SALE));
        stats.put("soldVehicles", vehicleRepository.countByVehicleStatus(VehicleStatus.SOLD));

        // 거래 통계
        stats.put("totalTransactions", transactionRepository.count());
        stats.put("completedTransactions", transactionRepository.countByTransactionStatus(TransactionStatus.COMPLETED));

        // 정산 통계
        stats.put("pendingSettlements", settlementRepository.countBySettlementStatus(SettlementStatus.PENDING));
        stats.put("completedSettlements", settlementRepository.countBySettlementStatus(SettlementStatus.COMPLETED));

        // 신고 통계
        stats.put("pendingReports", reportRepository.countByReportStatus(ReportStatus.PENDING));

        return stats;
    }

    /**
     * 매물 통계 상세 [ADM-006]
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getVehicleStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("pending", vehicleRepository.countByVehicleStatus(VehicleStatus.PENDING));
        stats.put("sale", vehicleRepository.countByVehicleStatus(VehicleStatus.SALE));
        stats.put("sold", vehicleRepository.countByVehicleStatus(VehicleStatus.SOLD));
        stats.put("expired", vehicleRepository.countByVehicleStatus(VehicleStatus.EXPIRED));
        stats.put("rejected", vehicleRepository.countByVehicleStatus(VehicleStatus.REJECTED));
        return stats;
    }

    /**
     * 거래 통계 상세 [ADM-007]
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getTransactionStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("requested", transactionRepository.countByTransactionStatus(TransactionStatus.REQUESTED));
        stats.put("approved", transactionRepository.countByTransactionStatus(TransactionStatus.APPROVED));
        stats.put("completed", transactionRepository.countByTransactionStatus(TransactionStatus.COMPLETED));
        stats.put("cancelled", transactionRepository.countByTransactionStatus(TransactionStatus.CANCELLED));
        return stats;
    }

    /**
     * 신고 통계 상세 [ADM-008]
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getReportStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("pending", reportRepository.countByReportStatus(ReportStatus.PENDING));
        stats.put("reviewing", reportRepository.countByReportStatus(ReportStatus.REVIEWING));
        stats.put("resolved", reportRepository.countByReportStatus(ReportStatus.RESOLVED));
        stats.put("rejected", reportRepository.countByReportStatus(ReportStatus.REJECTED));
        return stats;
    }
}