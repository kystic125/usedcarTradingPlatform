package com.usedcar.trading.domain.report.entity;

import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.global.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportType reportType;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReportStatus reportStatus = ReportStatus.PENDING;

    private String adminMemo;

    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    private User handler;

    /**
     * 신고 처리 (관리자)
     */
    public void resolve(User admin, String memo) {
        this.reportStatus = ReportStatus.RESOLVED;
        this.handler = admin;
        this.adminMemo = memo;
        this.resolvedAt = LocalDateTime.now();
    }

    /**
     * 신고 반려 (관리자)
     */
    public void reject(User admin, String memo) {
        this.reportStatus = ReportStatus.REJECTED;
        this.handler = admin;
        this.adminMemo = memo;
        this.resolvedAt = LocalDateTime.now();
    }

    /**
     * 검토중 상태로 변경
     */
    public void startReview(User admin) {
        this.reportStatus = ReportStatus.REVIEWING;
        this.handler = admin;
    }
}
