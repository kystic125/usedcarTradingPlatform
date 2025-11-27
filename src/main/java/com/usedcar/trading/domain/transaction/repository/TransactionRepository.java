package com.usedcar.trading.domain.transaction.repository;

import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.transaction.entity.Transaction;
import com.usedcar.trading.domain.transaction.entity.TransactionStatus;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // 회원 구매 목록(최신순)
    List<Transaction> findByBuyerOrderByCreatedAtDesc(User buyer);

    // 회사 판매 목록
    List<Transaction> findByCompany(Company company);

    // 자동차 거래 내역
    List<Transaction> findByVehicle(Vehicle vehicle);

    // 자동차 거래 횟수
    long countByVehicle(Vehicle vehicle);

    // 상태별 물품 조회
    List<Transaction> findByTransactionStatus(TransactionStatus status);

    // 특정 기간 내 거래된 물품 조회
    List<Transaction> findByCompletedAtBetween(LocalDateTime start, LocalDateTime end);

    // 상태별 물품 갯수 조회
    long countByTransactionStatus(TransactionStatus status);

    // 구매자, 상태별 조회
    List<Transaction> findByBuyerAndTransactionStatus(User buyer, TransactionStatus status);

    // 리뷰 가능한 물품 조회(구매 확정한 물품)
    @Query("SELECT t FROM Transaction t WHERE t.buyer = :buyer AND t.transactionStatus = 'COMPLETED' and t.review IS NULL")
    List<Transaction> findReviewableTransactionsByBuyer(@Param("buyer") User buyer);

    // 정산 대기중인 물품 조회
    @Query("SELECT t FROM Transaction t WHERE t.company = :company AND t.transactionStatus = 'COMPLETED' AND t.settlement IS NULL")
    List<Transaction> findPendingSettlementsByCompany(@Param("company") Company company);

    boolean existsByBuyerAndVehicleAndTransactionStatus(User buyer, Vehicle vehicle, TransactionStatus status);
}
