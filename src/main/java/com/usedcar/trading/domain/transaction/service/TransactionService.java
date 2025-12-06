package com.usedcar.trading.domain.transaction.service;

import com.usedcar.trading.domain.notification.entity.NotificationType;
import com.usedcar.trading.domain.notification.service.NotificationService;
import com.usedcar.trading.domain.settlement.entity.Settlement;
import com.usedcar.trading.domain.settlement.entity.SettlementStatus;
import com.usedcar.trading.domain.settlement.repository.SettlementRepository;
import com.usedcar.trading.domain.transaction.entity.Transaction;
import com.usedcar.trading.domain.transaction.entity.TransactionStatus;
import com.usedcar.trading.domain.transaction.repository.TransactionRepository;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.domain.vehicle.entity.VehicleStatus;
import com.usedcar.trading.domain.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final VehicleRepository vehicleRepository;
    private final SettlementRepository settlementRepository;
    private final NotificationService notificationService;

    // 거래 요청 (구매자)
    public Long requestTransaction(Long vehicleId, User buyer) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("매물을 찾을 수 없습니다."));

        // 1. 판매 중인 매물인지 확인
        if (vehicle.getVehicleStatus() != VehicleStatus.SALE) {
            throw new IllegalStateException("현재 판매 중인 차량이 아닙니다.");
        }

        // 2. 자가 거래 방지
        if (isSelfTrading(buyer, vehicle)) {
            throw new IllegalStateException("본인이 속한 회사의 차량은 구매할 수 없습니다.");
        }

        // 3. 중복 요청 방지
        if (transactionRepository.existsByBuyerAndVehicleAndTransactionStatus(
                buyer, vehicle, TransactionStatus.REQUESTED)) {
            throw new IllegalStateException("이미 구매 요청을 보낸 차량입니다. 판매자의 응답을 기다려주세요.");
        }

        Transaction transaction = Transaction.builder()
                .buyer(buyer)
                .company(vehicle.getCompany())
                .vehicle(vehicle)
                .price(vehicle.getPrice())
                .transactionStatus(TransactionStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();

        Long txnId = transactionRepository.save(transaction).getTransactionId();

        // 판매자(담당 딜러)에게 알림 발송
        User dealer = vehicle.getRegisteredBy().getUser();
        notificationService.createNotification(
                dealer,
                NotificationType.TRANSACTION_REQUEST,
                String.format("'%s' 차량에 대한 구매 요청이 들어왔습니다.", vehicle.getModel()),
                "/company/sales"
        );

        // 사장님에게도 알림 발송
        User boss = vehicle.getCompany().getOwner();
        if (!dealer.getUserId().equals(boss.getUserId())) {
            notificationService.createNotification(
                    boss,
                    NotificationType.TRANSACTION_REQUEST,
                    String.format("'%s' (담당: %s) 차량에 구매 요청이 있습니다.", vehicle.getModel(), dealer.getName()),
                    "/company/sales"
            );
        }
        return txnId;
    }

    // 거래 승인 (판매자)
    public void approveTransaction(Long transactionId, User seller) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래 내역이 없습니다."));

        validateSeller(transaction, seller);

        if (transaction.getVehicle().getVehicleStatus() != VehicleStatus.SALE) {
            throw new IllegalStateException("이미 예약되었거나 판매된 차량이라 승인할 수 없습니다.");
        }

        if (transaction.getTransactionStatus() != TransactionStatus.REQUESTED) {
            throw new IllegalStateException("대기 상태의 거래만 승인할 수 있습니다.");
        }

        // 1. 거래 상태 승인으로 변경
        transaction.updateStatus(TransactionStatus.APPROVED);

        // 2. 매물 상태 예약중으로 변경
        transaction.getVehicle().changeStatus(VehicleStatus.RESERVED);

        notificationService.createNotification(
                transaction.getBuyer(),
                NotificationType.TRANSACTION_APPROVED,
                String.format("'%s' 구매 요청이 승인되었습니다. 판매자와 연락하여 거래를 진행하세요.", transaction.getVehicle().getModel()),
                "/mypage/purchases"
        );
    }

    // 거래 거부 (판매자)
    public void rejectTransaction(Long transactionId, User seller) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래 내역이 없습니다."));

        validateSeller(transaction, seller);

        if (transaction.getTransactionStatus() != TransactionStatus.REQUESTED) {
            throw new IllegalStateException("대기 상태의 거래만 거부할 수 있습니다.");
        }

        transaction.updateStatus(TransactionStatus.REJECTED);

        notificationService.createNotification(
                transaction.getBuyer(),
                NotificationType.TRANSACTION_REJECTED,
                String.format("'%s' 구매 요청이 거절되었습니다.", transaction.getVehicle().getModel()),
                "/mypage/purchases"
        );
    }

    // 거래 취소 (구매자/판매자 모두 가능)
    public void cancelTransaction(Long transactionId, User user) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래 내역이 없습니다."));

        boolean isBuyer = transaction.getBuyer().getUserId().equals(user.getUserId());
        boolean isSeller = transaction.getCompany().getOwner().getUserId().equals(user.getUserId());

        if (!isBuyer && !isSeller) {
            throw new IllegalArgumentException("거래 취소 권한이 없습니다.");
        }

        if (transaction.getTransactionStatus() == TransactionStatus.COMPLETED) {
            throw new IllegalStateException("이미 완료된 거래는 취소할 수 없습니다.");
        }

        transaction.updateStatus(TransactionStatus.CANCELLED);

        if (transaction.getVehicle().getVehicleStatus() == VehicleStatus.RESERVED) {
            transaction.getVehicle().changeStatus(VehicleStatus.SALE);
        }

        User targetUser = isBuyer ? transaction.getVehicle().getRegisteredBy().getUser() : transaction.getBuyer();
        String whoCancelled = isBuyer ? "구매자" : "판매자";

        notificationService.createNotification(
                targetUser,
                NotificationType.TRANSACTION_CANCELLED,
                String.format("'%s' 거래가 %s에 의해 취소되었습니다.", transaction.getVehicle().getModel(), whoCancelled),
                isBuyer ? "/company/sales" : "/mypage/purchases"
        );
    }

    // 거래 완료 (판매자 -> 차량 인도 완료)
    public void completeTransaction(Long transactionId, User seller) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래 내역이 없습니다."));

        // 1. 권한 체크
        validateSeller(transaction, seller);

        // 2. 상태 체크
        if (transaction.getTransactionStatus() != TransactionStatus.APPROVED) {
            throw new IllegalStateException("진행 중(승인된) 거래만 완료 처리할 수 있습니다.");
        }

        // 3. 거래 상태 변경
        transaction.updateStatus(TransactionStatus.COMPLETED);

        // 4. 매물 상태 변경
        transaction.getVehicle().changeStatus(VehicleStatus.SOLD);

        // 5. 정산 데이터 자동 생성
        createSettlement(transaction);

        notificationService.createNotification(
                transaction.getBuyer(),
                NotificationType.TRANSACTION_COMPLETED,
                "거래가 완료되었습니다. 만족하셨다면 리뷰를 작성해주세요!",
                "/mypage/purchases"
        );

        log.info("거래 완료 처리됨: txnId={}", transactionId);
    }

    // 정산 생성 로직
    private void createSettlement(Transaction transaction) {
        // 수수료 정책: 2.2% (0.022)
        BigDecimal commissionRate = new BigDecimal("0.022");

        // 수수료 계산 = 가격 * 0.022
        BigDecimal commissionAmount = transaction.getPrice().multiply(commissionRate);

        // 정산 받을 금액 = 가격 - 수수료
        BigDecimal settlementAmount = transaction.getPrice().subtract(commissionAmount);

        // 정산 엔티티 생성
        Settlement settlement = Settlement.builder()
                .company(transaction.getCompany())
                .transaction(transaction)
                .totalAmount(transaction.getPrice())
                .commissionAmount(commissionAmount)
                .settlementAmount(settlementAmount)
                .settlementStatus(SettlementStatus.PENDING)
                .build();

        // 저장
        settlementRepository.save(settlement);
    }

    private void validateSeller(Transaction transaction, User seller) {
        if (!transaction.getCompany().getOwner().getUserId().equals(seller.getUserId())) {
            throw new IllegalArgumentException("해당 거래를 승인/거부할 권한이 없습니다.");
        }
    }

    private boolean isSelfTrading(User buyer, Vehicle vehicle) {
        Long buyerCompanyId = null;

        // 구매자가 사장님인 경우
        if (buyer.getRole() == com.usedcar.trading.domain.user.entity.Role.COMPANY_OWNER) {
            if(vehicle.getCompany().getOwner().getUserId().equals(buyer.getUserId()))
                return true;
        }

        // 구매자가 직원인 경우
        if (buyer.getRole() == com.usedcar.trading.domain.user.entity.Role.COMPANY_EMPLOYEE) {
            if (buyer.getEmployee() != null &&
                    buyer.getEmployee().getCompany().getCompanyId().equals(vehicle.getCompany().getCompanyId())) {
                return true;
            }
        }

        return false;
    }
}