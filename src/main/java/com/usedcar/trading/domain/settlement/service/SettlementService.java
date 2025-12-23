package com.usedcar.trading.domain.settlement.service;

import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.company.repository.CompanyRepository;
import com.usedcar.trading.domain.employee.entity.Employee;
import com.usedcar.trading.domain.employee.repository.EmployeeRepository;
import com.usedcar.trading.domain.notification.entity.NotificationType;
import com.usedcar.trading.domain.notification.service.NotificationService;
import com.usedcar.trading.domain.settlement.entity.Settlement;
import com.usedcar.trading.domain.settlement.entity.SettlementStatus;
import com.usedcar.trading.domain.settlement.repository.SettlementRepository;
import com.usedcar.trading.domain.user.entity.Role;
import com.usedcar.trading.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final CompanyRepository companyRepository;
    private final NotificationService notificationService;
    private final EmployeeRepository employeeRepository;

    // 업체 정산 목록 조회
    public List<Settlement> getCompanySettlements(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("업체를 찾을 수 없습니다."));
        return settlementRepository.findByCompany(company);
    }

    // 업체 기간별 정산 목록 조회
    public List<Settlement> getCompanySettlementsByPeriod(Long companyId, LocalDateTime start, LocalDateTime end) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("업체를 찾을 수 없습니다."));
        return settlementRepository.findByCompanyAndSettledAtBetween(company, start, end);
    }

    // 업체 총 정산액 조회
    public BigDecimal getTotalSettlementAmount(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("업체를 찾을 수 없습니다."));
        BigDecimal total = settlementRepository.getTotalSettlementAmountByCompany(company);
        return total != null ? total : BigDecimal.ZERO;
    }

    // 업체 기간별 총 정산액 조회
    public BigDecimal getTotalSettlementAmountByPeriod(Long companyId, LocalDateTime start, LocalDateTime end) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("업체를 찾을 수 없습니다."));
        BigDecimal total = settlementRepository.getTotalSettlementAmountByCompanyAndPeriod(company, start, end);
        return total != null ? total : BigDecimal.ZERO;
    }

    // 정산 상세 조회
    public Settlement getSettlement(Long settlementId) {
        return settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("정산을 찾을 수 없습니다."));
    }

    // 정산 완료 처리 (관리자)
    @Transactional
    public void completeSettlement(Long settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("정산을 찾을 수 없습니다."));

        if (settlement.getSettlementStatus() != SettlementStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 정산입니다.");
        }

        settlement.complete();

        // 업체 대표에게 알림
        notificationService.createNotification(
                settlement.getCompany().getOwner(),
                NotificationType.SETTLEMENT_COMPLETED,
                String.format("정산이 완료되었습니다. (정산액: %s원)", settlement.getSettlementAmount()),
                "/settlements/" + settlementId
        );

        log.info("정산 완료: settlementId={}, amount={}", settlementId, settlement.getSettlementAmount());
    }

    // 상태별 정산 목록 조회 (관리자)
    public List<Settlement> getSettlementsByStatus(SettlementStatus status) {
        return settlementRepository.findBySettlementStatusOrderByCreatedAtDesc(status);
    }

    // 대기중 정산 개수 조회 (관리자)
    public long getPendingSettlementCount() {
        return settlementRepository.countBySettlementStatus(SettlementStatus.PENDING);
    }

    public Page<Settlement> getMySettlements(User user, Pageable pageable) {
        Company company = findUserCompany(user);
        return settlementRepository.findByCompany(company, pageable);
    }

    private Company findUserCompany(User user) {
        if (user.getRole() == Role.COMPANY_OWNER) {
            return companyRepository.findByOwner_UserId(user.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("업체 정보가 없습니다."));
        } else if (user.getRole() == Role.COMPANY_EMPLOYEE) {
            return employeeRepository.findByUserUserId(user.getUserId())
                    .map(Employee::getCompany)
                    .orElseThrow(() -> new IllegalArgumentException("소속된 회사가 없습니다."));
        }
        throw new IllegalStateException("판매자 권한이 없습니다.");
    }
}
