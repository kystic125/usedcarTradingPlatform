package com.usedcar.trading.domain.settlement.repository;

import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.settlement.entity.Settlement;
import com.usedcar.trading.domain.settlement.entity.SettlementStatus;
import com.usedcar.trading.domain.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    // Company 정산 리스트 조회
    List<Settlement> findByCompany(Company company);

    // company 정산 상태 필터링
    List<Settlement> findBySettlementStatus(SettlementStatus status);

    // 정산 상태별 최신순 정렬
    List<Settlement> findBySettlementStatusOrderByCreatedAtDesc(SettlementStatus status);

    // transaction 으로 settlement 찾기
    Optional<Settlement> findByTransaction(Transaction transaction);

    // 정산 상태별 갯수 조회
    long countBySettlementStatus(SettlementStatus status);

    // 업체의 총 정산액 (COMPLETED만)
    @Query("SELECT SUM(s.settlementAmount) FROM Settlement s WHERE s.company =:company AND s.settlementStatus = 'COMPLETED'")
    BigDecimal getTotalSettlementAmountByCompany(@Param("company") Company company);

    // 특정 기간동안 업체의 총 정산액
    @Query("SELECT SUM(s.settlementAmount) FROM Settlement s WHERE s.company = :company AND s.settledAt BETWEEN :start AND :end AND s.settlementStatus = 'COMPLETED'")
    BigDecimal getTotalSettlementAmountByCompanyAndPeriod(
            @Param("company") Company company,
            @Param("start")LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // 업체 + 기간별 정산 목록 조회
    List<Settlement> findByCompanyAndSettledAtBetween(Company company, LocalDateTime start, LocalDateTime end);
}
