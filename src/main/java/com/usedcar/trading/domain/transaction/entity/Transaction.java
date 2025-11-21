package com.usedcar.trading.domain.transaction.entity;

import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.review.entity.Review;
import com.usedcar.trading.domain.settlement.entity.Settlement;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.global.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus transactionStatus = TransactionStatus.REQUESTED;

    @Column(nullable = false)
    private BigDecimal price;

    private BigDecimal commissionAmount;

    private BigDecimal sellerAmount;

    private LocalDateTime requestedAt;

    private LocalDateTime approvedAt;

    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @OneToOne(mappedBy = "transaction")
    private Settlement settlement;

    @OneToOne(mappedBy = "transaction")
    private Review review;
}
