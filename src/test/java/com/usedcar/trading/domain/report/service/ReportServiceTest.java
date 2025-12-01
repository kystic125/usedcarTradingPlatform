package com.usedcar.trading.domain.report.service;

import com.usedcar.trading.domain.notification.service.NotificationService;
import com.usedcar.trading.domain.report.entity.Report;
import com.usedcar.trading.domain.report.entity.ReportStatus;
import com.usedcar.trading.domain.report.entity.ReportType;
import com.usedcar.trading.domain.report.repository.ReportRepository;
import com.usedcar.trading.domain.user.entity.Role;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReportService reportService;

    // 테스트용 공통 객체
    private User reporter;
    private User admin;
    private Report report;

    @BeforeEach
    void setUp() {
        // 신고자 (일반 사용자)
        reporter = User.builder()
                .userId(1L)
                .email("reporter@test.com")
                .name("신고자")
                .password("password")
                .phone("010-1234-5678")
                .role(Role.CUSTOMER)
                .build();

        // 관리자
        admin = User.builder()
                .userId(100L)
                .email("admin@test.com")
                .name("관리자")
                .password("password")
                .phone("010-0000-0000")
                .role(Role.ADMIN)
                .build();

        // 신고 (PENDING 상태)
        report = Report.builder()
                .reportId(1L)
                .reporter(reporter)
                .reportType(ReportType.VEHICLE)
                .targetId(10L)
                .description("허위 매물입니다")
                .reportStatus(ReportStatus.PENDING)
                .build();
    }

    // ==================== 신고 등록 테스트 ====================
    @Nested
    @DisplayName("신고 등록 (createReport)")
    class CreateReport {

        @Test
        @DisplayName("성공: 신고 등록")
        void 신고등록_성공() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(reporter));
            given(reportRepository.existsByReporterAndReportTypeAndTargetId(reporter, ReportType.VEHICLE, 10L))
                    .willReturn(false);
            given(reportRepository.save(any(Report.class)))
                    .willAnswer(invocation -> {
                        Report saved = invocation.getArgument(0);
                        return Report.builder()
                                .reportId(100L)
                                .reporter(saved.getReporter())
                                .reportType(saved.getReportType())
                                .targetId(saved.getTargetId())
                                .description(saved.getDescription())
                                .build();
                    });

            // when
            Report result = reportService.createReport(1L, ReportType.VEHICLE, 10L, "허위 매물입니다");

            // then
            assertThat(result.getReportId()).isEqualTo(100L);
            assertThat(result.getReportType()).isEqualTo(ReportType.VEHICLE);
            assertThat(result.getTargetId()).isEqualTo(10L);
            verify(reportRepository, times(1)).save(any(Report.class));
        }

        @Test
        @DisplayName("실패: 중복 신고")
        void 신고등록_실패_중복신고() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(reporter));
            given(reportRepository.existsByReporterAndReportTypeAndTargetId(reporter, ReportType.VEHICLE, 10L))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> reportService.createReport(1L, ReportType.VEHICLE, 10L, "허위 매물입니다"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 신고한 대상입니다");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자")
        void 신고등록_실패_사용자없음() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reportService.createReport(999L, ReportType.VEHICLE, 10L, "허위 매물입니다"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }
    }

    // ==================== 신고 조회 테스트 ====================
    @Nested
    @DisplayName("신고 조회 (getReport)")
    class GetReport {

        @Test
        @DisplayName("성공: 신고 상세 조회")
        void 신고조회_성공() {
            // given
            given(reportRepository.findById(1L)).willReturn(Optional.of(report));

            // when
            Report result = reportService.getReport(1L);

            // then
            assertThat(result.getReportId()).isEqualTo(1L);
            assertThat(result.getReportType()).isEqualTo(ReportType.VEHICLE);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 신고")
        void 신고조회_실패_신고없음() {
            // given
            given(reportRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reportService.getReport(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("신고를 찾을 수 없습니다");
        }
    }

    // ==================== 신고 처리 테스트 ====================
    @Nested
    @DisplayName("신고 처리 (resolveReport)")
    class ResolveReport {

        @Test
        @DisplayName("성공: 신고 처리 완료")
        void 신고처리_성공() {
            // given
            given(reportRepository.findById(1L)).willReturn(Optional.of(report));
            given(userRepository.findById(100L)).willReturn(Optional.of(admin));

            // when
            reportService.resolveReport(1L, 100L, "확인 후 조치 완료");

            // then
            assertThat(report.getReportStatus()).isEqualTo(ReportStatus.RESOLVED);
            assertThat(report.getHandler()).isEqualTo(admin);
            assertThat(report.getAdminMemo()).isEqualTo("확인 후 조치 완료");
            assertThat(report.getResolvedAt()).isNotNull();
            verify(notificationService, times(1)).createNotification(any(), any(), any(), any());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 신고")
        void 신고처리_실패_신고없음() {
            // given
            given(reportRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reportService.resolveReport(999L, 100L, "처리 완료"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("신고를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 관리자")
        void 신고처리_실패_관리자없음() {
            // given
            given(reportRepository.findById(1L)).willReturn(Optional.of(report));
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reportService.resolveReport(1L, 999L, "처리 완료"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("관리자를 찾을 수 없습니다");
        }
    }

    // ==================== 신고 반려 테스트 ====================
    @Nested
    @DisplayName("신고 반려 (rejectReport)")
    class RejectReport {

        @Test
        @DisplayName("성공: 신고 반려")
        void 신고반려_성공() {
            // given
            given(reportRepository.findById(1L)).willReturn(Optional.of(report));
            given(userRepository.findById(100L)).willReturn(Optional.of(admin));

            // when
            reportService.rejectReport(1L, 100L, "신고 사유 불충분");

            // then
            assertThat(report.getReportStatus()).isEqualTo(ReportStatus.REJECTED);
            assertThat(report.getHandler()).isEqualTo(admin);
            assertThat(report.getAdminMemo()).isEqualTo("신고 사유 불충분");
            assertThat(report.getResolvedAt()).isNotNull();
            verify(notificationService, times(1)).createNotification(any(), any(), any(), any());
        }
    }

    // ==================== 내 신고 목록 조회 테스트 ====================
    @Nested
    @DisplayName("내 신고 목록 조회 (getMyReports)")
    class GetMyReports {

        @Test
        @DisplayName("성공: 내 신고 목록 조회")
        void 내신고목록_성공() {
            // given
            Report report2 = Report.builder()
                    .reportId(2L)
                    .reporter(reporter)
                    .reportType(ReportType.COMPANY)
                    .targetId(20L)
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(reporter));
            given(reportRepository.findByReporter(reporter))
                    .willReturn(Arrays.asList(report, report2));

            // when
            List<Report> result = reportService.getMyReports(1L);

            // then
            assertThat(result).hasSize(2);
        }
    }

    // ==================== 상태별 신고 목록 조회 테스트 ====================
    @Nested
    @DisplayName("상태별 신고 목록 조회 (getReportsByStatus)")
    class GetReportsByStatus {

        @Test
        @DisplayName("성공: PENDING 상태 신고 목록 조회")
        void 상태별신고목록_성공() {
            // given
            given(reportRepository.findByReportStatusOrderByCreatedAtDesc(ReportStatus.PENDING))
                    .willReturn(Arrays.asList(report));

            // when
            List<Report> result = reportService.getReportsByStatus(ReportStatus.PENDING);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getReportStatus()).isEqualTo(ReportStatus.PENDING);
        }
    }

    // ==================== 미처리 신고 개수 조회 테스트 ====================
    @Nested
    @DisplayName("미처리 신고 개수 조회 (getPendingReportCount)")
    class GetPendingReportCount {

        @Test
        @DisplayName("성공: 미처리 신고 개수 조회")
        void 미처리신고개수_성공() {
            // given
            given(reportRepository.countByReportStatus(ReportStatus.PENDING))
                    .willReturn(3L);

            // when
            long result = reportService.getPendingReportCount();

            // then
            assertThat(result).isEqualTo(3L);
        }
    }

    // ==================== 대상별 신고 개수 조회 테스트 ====================
    @Nested
    @DisplayName("대상별 신고 개수 조회 (getReportCountByTarget)")
    class GetReportCountByTarget {

        @Test
        @DisplayName("성공: 특정 차량 신고 개수 조회")
        void 대상별신고개수_성공() {
            // given
            given(reportRepository.countByReportTypeAndTargetId(ReportType.VEHICLE, 10L))
                    .willReturn(5L);

            // when
            long result = reportService.getReportCountByTarget(ReportType.VEHICLE, 10L);

            // then
            assertThat(result).isEqualTo(5L);
        }
    }
}
