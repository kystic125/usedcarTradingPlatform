package com.usedcar.trading.domain.report.service;

import com.usedcar.trading.domain.notification.entity.NotificationType;
import com.usedcar.trading.domain.notification.service.NotificationService;
import com.usedcar.trading.domain.report.entity.Report;
import com.usedcar.trading.domain.report.entity.ReportStatus;
import com.usedcar.trading.domain.report.entity.ReportType;
import com.usedcar.trading.domain.report.repository.ReportRepository;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * 신고 등록 [RPT-001]
     */
    @Transactional
    public Report createReport(Long reporterId, ReportType type, Long targetId, String description) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 중복 신고 확인
        if (reportRepository.existsByReporterAndReportTypeAndTargetId(reporter, type, targetId)) {
            throw new IllegalStateException("이미 신고한 대상입니다.");
        }

        Report report = Report.builder()
                .reporter(reporter)
                .reportType(type)
                .targetId(targetId)
                .description(description)
                .build();

        Report savedReport = reportRepository.save(report);
        log.info("신고 등록: reportId={}, type={}, targetId={}", savedReport.getReportId(), type, targetId);

        return savedReport;
    }

    /**
     * 신고 상세 조회 [RPT-002]
     */
    public Report getReport(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다."));
    }

    /**
     * 내 신고 목록 조회 [RPT-003]
     */
    public List<Report> getMyReports(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return reportRepository.findByReporter(user);
    }

    /**
     * 신고 처리 (관리자) [RPT-004]
     */
    @Transactional
    public void resolveReport(Long reportId, Long adminId, String memo) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다."));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));

        report.resolve(admin, memo);

        // 신고자에게 처리 완료 알림
        notificationService.createNotification(
                report.getReporter(),
                NotificationType.REPORT_PROCESSED,
                "접수하신 신고가 처리되었습니다.",
                "/reports/" + reportId
        );

        log.info("신고 처리 완료: reportId={}, adminId={}", reportId, adminId);
    }

    /**
     * 신고 반려 (관리자) [RPT-005]
     */
    @Transactional
    public void rejectReport(Long reportId, Long adminId, String memo) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다."));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));

        report.reject(admin, memo);

        // 신고자에게 반려 알림
        notificationService.createNotification(
                report.getReporter(),
                NotificationType.REPORT_PROCESSED,
                "접수하신 신고가 반려되었습니다.",
                "/reports/" + reportId
        );

        log.info("신고 반려: reportId={}, adminId={}", reportId, adminId);
    }

    /**
     * 상태별 신고 목록 조회 (관리자)
     */
    public List<Report> getReportsByStatus(ReportStatus status) {
        return reportRepository.findByReportStatusOrderByCreatedAtDesc(status);
    }

    /**
     * 미처리 신고 개수 조회 (관리자)
     */
    public long getPendingReportCount() {
        return reportRepository.countByReportStatus(ReportStatus.PENDING);
    }

    /**
     * 대상별 신고 개수 조회
     */
    public long getReportCountByTarget(ReportType type, Long targetId) {
        return reportRepository.countByReportTypeAndTargetId(type, targetId);
    }
}
