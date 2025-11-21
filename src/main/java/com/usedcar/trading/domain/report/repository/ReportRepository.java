package com.usedcar.trading.domain.report.repository;

import com.usedcar.trading.domain.report.entity.Report;
import com.usedcar.trading.domain.report.entity.ReportStatus;
import com.usedcar.trading.domain.report.entity.ReportType;
import com.usedcar.trading.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    // 1. 신고자 기준 조회
    // 회원 id를 기준으로 신고한 목록 조회
    List<Report> findByReporter(User reporter);

    // 2. 관리자 관점
    // 상태별 신고 조회
    List<Report> findByReportStatus(ReportStatus reportStatus);

    // 미처리 신고 개수
    long countByReportStatus(ReportStatus reportStatus);

    // 처리자별 신고 목록 (어떤 관리자가 처리했는지)
    List<Report> findByHandler(User handler);

    // 신고 유형별 조회
    List<Report> findByReportType(ReportType type);

    // 3. 신고 대상별 조회
    // 대상 id를 기준으로 신고당한 목록 조회
    List<Report> findByReportTypeAndTargetId(ReportType type, Long targetId);

    // 신고 개수 조회 (신뢰도 계산용)
    long countByReportTypeAndTargetId(ReportType type, Long targetId);

    // 4. 중복 신고 방지
    // 회원id, 대상id를 기준으로 같은 사람이 중복 신고하는지 확인
    boolean existsByReporterAndReportTypeAndTargetId(
            User reporter, ReportType type, Long targetId
    );

    // 신고를 최신순으로 보고 싶을 때
    List<Report> findByReportStatusOrderByCreatedAtDesc(ReportStatus reportStatus);
}
