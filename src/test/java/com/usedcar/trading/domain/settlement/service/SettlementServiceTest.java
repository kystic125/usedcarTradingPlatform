package com.usedcar.trading.domain.settlement.service;

import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.company.repository.CompanyRepository;
import com.usedcar.trading.domain.notification.service.NotificationService;
import com.usedcar.trading.domain.settlement.entity.Settlement;
import com.usedcar.trading.domain.settlement.entity.SettlementStatus;
import com.usedcar.trading.domain.settlement.repository.SettlementRepository;
import com.usedcar.trading.domain.transaction.entity.Transaction;
import com.usedcar.trading.domain.user.entity.Role;
import com.usedcar.trading.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SettlementService settlementService;

    // 테스트용 공통 객체
    private User owner;
    private Company company;
    private Transaction transaction;
    private Settlement settlement;

    @BeforeEach
    void setUp() {
        // 업체 소유자
        owner = User.builder()
                .userId(1L)
                .email("owner@test.com")
                .name("업체소유자")
                .password("password")
                .phone("010-1234-5678")
                .role(Role.COMPANY_OWNER)
                .build();

        // 회사
        company = Company.builder()
                .companyId(1L)
                .businessName("테스트 중고차")
                .businessNumber("123-45-67890")
                .address("서울시 강남구")
                .owner(owner)
                .build();

        // 거래
        transaction = Transaction.builder()
                .transactionId(1L)
                .company(company)
                .price(new BigDecimal("25000000"))
                .build();

        // 정산 (PENDING 상태)
        settlement = Settlement.builder()
                .settlementId(1L)
                .company(company)
                .transaction(transaction)
                .totalAmount(new BigDecimal("25000000"))
                .commissionAmount(new BigDecimal("550000"))
                .settlementAmount(new BigDecimal("24450000"))
                .settlementStatus(SettlementStatus.PENDING)
                .build();
    }

    // ==================== 정산 완료 테스트 ====================
    @Nested
    @DisplayName("정산 완료 처리 (completeSettlement)")
    class CompleteSettlement {

        @Test
        @DisplayName("성공: PENDING 상태 정산 완료 처리")
        void 정산완료_성공() {
            // given
            given(settlementRepository.findById(1L)).willReturn(Optional.of(settlement));

            // when
            settlementService.completeSettlement(1L);

            // then
            assertThat(settlement.getSettlementStatus()).isEqualTo(SettlementStatus.COMPLETED);
            assertThat(settlement.getSettledAt()).isNotNull();
            verify(notificationService, times(1)).createNotification(any(), any(), any(), any());
        }

        @Test
        @DisplayName("실패: 이미 처리된 정산")
        void 정산완료_실패_이미처리됨() {
            // given
            settlement.complete();  // COMPLETED로 변경
            given(settlementRepository.findById(1L)).willReturn(Optional.of(settlement));

            // when & then
            assertThatThrownBy(() -> settlementService.completeSettlement(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 처리된 정산");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 정산")
        void 정산완료_실패_정산없음() {
            // given
            given(settlementRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> settlementService.completeSettlement(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("정산을 찾을 수 없습니다");
        }
    }

    // ==================== 정산 조회 테스트 ====================
    @Nested
    @DisplayName("정산 조회 (getSettlement)")
    class GetSettlement {

        @Test
        @DisplayName("성공: 정산 상세 조회")
        void 정산조회_성공() {
            // given
            given(settlementRepository.findById(1L)).willReturn(Optional.of(settlement));

            // when
            Settlement result = settlementService.getSettlement(1L);

            // then
            assertThat(result.getSettlementId()).isEqualTo(1L);
            assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("25000000"));
            assertThat(result.getCommissionAmount()).isEqualByComparingTo(new BigDecimal("550000"));
            assertThat(result.getSettlementAmount()).isEqualByComparingTo(new BigDecimal("24450000"));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 정산")
        void 정산조회_실패_정산없음() {
            // given
            given(settlementRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> settlementService.getSettlement(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("정산을 찾을 수 없습니다");
        }
    }

    // ==================== 업체 정산 목록 조회 테스트 ====================
    @Nested
    @DisplayName("업체 정산 목록 조회 (getCompanySettlements)")
    class GetCompanySettlements {

        @Test
        @DisplayName("성공: 업체 정산 목록 조회")
        void 업체정산목록_성공() {
            // given
            Settlement settlement2 = Settlement.builder()
                    .settlementId(2L)
                    .company(company)
                    .totalAmount(new BigDecimal("30000000"))
                    .commissionAmount(new BigDecimal("660000"))
                    .settlementAmount(new BigDecimal("29340000"))
                    .settlementStatus(SettlementStatus.COMPLETED)
                    .build();

            given(companyRepository.findById(1L)).willReturn(Optional.of(company));
            given(settlementRepository.findByCompany(company))
                    .willReturn(Arrays.asList(settlement, settlement2));

            // when
            List<Settlement> result = settlementService.getCompanySettlements(1L);

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 업체")
        void 업체정산목록_실패_업체없음() {
            // given
            given(companyRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> settlementService.getCompanySettlements(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("업체를 찾을 수 없습니다");
        }
    }

    // ==================== 총 정산액 조회 테스트 ====================
    @Nested
    @DisplayName("총 정산액 조회 (getTotalSettlementAmount)")
    class GetTotalSettlementAmount {

        @Test
        @DisplayName("성공: 총 정산액 조회")
        void 총정산액_성공() {
            // given
            given(companyRepository.findById(1L)).willReturn(Optional.of(company));
            given(settlementRepository.getTotalSettlementAmountByCompany(company))
                    .willReturn(new BigDecimal("53790000"));  // 24450000 + 29340000

            // when
            BigDecimal result = settlementService.getTotalSettlementAmount(1L);

            // then
            assertThat(result).isEqualByComparingTo(new BigDecimal("53790000"));
        }

        @Test
        @DisplayName("성공: 정산 없을 때 0 반환")
        void 총정산액_정산없음() {
            // given
            given(companyRepository.findById(1L)).willReturn(Optional.of(company));
            given(settlementRepository.getTotalSettlementAmountByCompany(company))
                    .willReturn(null);  // SUM()은 결과 없으면 null

            // when
            BigDecimal result = settlementService.getTotalSettlementAmount(1L);

            // then
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ==================== 상태별 정산 목록 조회 테스트 ====================
    @Nested
    @DisplayName("상태별 정산 목록 조회 (getSettlementsByStatus)")
    class GetSettlementsByStatus {

        @Test
        @DisplayName("성공: PENDING 상태 정산 목록 조회")
        void 상태별정산목록_성공() {
            // given
            given(settlementRepository.findBySettlementStatusOrderByCreatedAtDesc(SettlementStatus.PENDING))
                    .willReturn(Arrays.asList(settlement));

            // when
            List<Settlement> result = settlementService.getSettlementsByStatus(SettlementStatus.PENDING);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSettlementStatus()).isEqualTo(SettlementStatus.PENDING);
        }
    }

    // ==================== 대기중 정산 개수 조회 테스트 ====================
    @Nested
    @DisplayName("대기중 정산 개수 조회 (getPendingSettlementCount)")
    class GetPendingSettlementCount {

        @Test
        @DisplayName("성공: 대기중 정산 개수 조회")
        void 대기중정산개수_성공() {
            // given
            given(settlementRepository.countBySettlementStatus(SettlementStatus.PENDING))
                    .willReturn(5L);

            // when
            long result = settlementService.getPendingSettlementCount();

            // then
            assertThat(result).isEqualTo(5L);
        }

        @Test
        @DisplayName("성공: 대기중 정산 없을 때 0 반환")
        void 대기중정산개수_없음() {
            // given
            given(settlementRepository.countBySettlementStatus(SettlementStatus.PENDING))
                    .willReturn(0L);

            // when
            long result = settlementService.getPendingSettlementCount();

            // then
            assertThat(result).isEqualTo(0L);
        }
    }
}
