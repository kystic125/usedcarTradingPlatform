package com.usedcar.trading.domain.transaction.service;

import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.employee.entity.Employee;
import com.usedcar.trading.domain.settlement.entity.Settlement;
import com.usedcar.trading.domain.settlement.repository.SettlementRepository;
import com.usedcar.trading.domain.transaction.entity.Transaction;
import com.usedcar.trading.domain.transaction.entity.TransactionStatus;
import com.usedcar.trading.domain.transaction.repository.TransactionRepository;
import com.usedcar.trading.domain.user.entity.Role;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.domain.vehicle.entity.VehicleStatus;
import com.usedcar.trading.domain.vehicle.repository.VehicleRepository;
import org.antlr.v4.runtime.misc.LogManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private SettlementRepository settlementRepository;

    @InjectMocks
    private TransactionService transactionService;

    // 테스트용 공통 객체
    private User buyer;
    private User seller;
    private Company company;
    private Vehicle vehicle;
    private Employee employee;

    @BeforeEach
    void setUp() {
        // 판매자 (회사 소유자)
        seller = User.builder()
                .userId(1L)
                .email("seller@test.com")
                .name("판매자")
                .password("password")
                .phone("010-1234-5678")
                .role(Role.COMPANY_OWNER)
                .build();

        // 구매자 (일반 고객)
        buyer = User.builder()
                .userId(2L)
                .email("buyer@test.com")
                .name("구매자")
                .password("password")
                .phone("010-9876-5432")
                .role(Role.CUSTOMER)
                .build();

        // 회사
        company = Company.builder()
                .companyId(1L)
                .businessName("테스트 중고차")
                .businessNumber("123-45-67890")
                .address("서울시 강남구")
                .owner(seller)
                .build();

        // 직원
        employee = Employee.builder()
                .employeeId(1L)
                .user(seller)
                .company(company)
                .build();

        // 차량 (판매 중 상태)
        vehicle = Vehicle.builder()
                .vehicleId(1L)
                .brand("현대")
                .model("아반떼")
                .modelYear(2023)
                .mileage(10000)
                .price(new BigDecimal("25000000"))
                .vehicleStatus(VehicleStatus.SALE)
                .company(company)
                .registeredBy(employee)
                .build();
    }

    // ==================== 거래 요청 테스트 ====================
    @Nested
    @DisplayName("거래 요청 (requestTransaction)")
    class RequestTransaction {

        @Test
        @DisplayName("성공: 판매 중인 차량에 거래 요청")
        void 거래요청_성공() {
            // given
            given(vehicleRepository.findById(1L)).willReturn(Optional.of(vehicle));
            given(transactionRepository.existsByBuyerAndVehicleAndTransactionStatus(
                    buyer, vehicle, TransactionStatus.REQUESTED)).willReturn(false);
            given(transactionRepository.save(any(Transaction.class)))
                    .willAnswer(invocation -> {
                        Transaction saved = invocation.getArgument(0);

                        return Transaction.builder()
                                .transactionId(100L)
                                .buyer(saved.getBuyer())
                                .vehicle(saved.getVehicle())
                                .company(saved.getCompany())
                                .price(saved.getPrice())
                                .transactionStatus(saved.getTransactionStatus())
                                .build();
                    });

            // when
            Long transactionId = transactionService.requestTransaction(1L, buyer);

            // then
            assertThat(transactionId).isEqualTo(100L);
            verify(transactionRepository, times(1)).save(any(Transaction.class));
        }

        @Test
        @DisplayName("실패: 중복 거래 요청")
        void 거래요청_실패_중복요청() {
            // given
            given(vehicleRepository.findById(1L)).willReturn(Optional.of(vehicle));
            given(transactionRepository.existsByBuyerAndVehicleAndTransactionStatus(
                    buyer, vehicle, TransactionStatus.REQUESTED)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> transactionService.requestTransaction(1L, buyer))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 구매 요청을 보낸 차량입니다");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 차량")
        void 거래요청_실패_차량없음() {
            // given
            given(vehicleRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> transactionService.requestTransaction(999L, buyer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("매물을 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("실패: 판매 중이 아닌 차량")
        void 거래요청_실패_판매중아님() {
            // given
            vehicle.changeStatus(VehicleStatus.RESERVED);
            given(vehicleRepository.findById(1L)).willReturn(Optional.of(vehicle));

            // when & then
            assertThatThrownBy(() -> transactionService.requestTransaction(1L, buyer))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("현재 판매 중인 차량이 아닙니다.");
        }

        @Test
        @DisplayName("실패: 자기 회사 차량 구매 시도")
        void 거래요청_실패_자가거래() {
            // given
            given(vehicleRepository.findById(1L)).willReturn(Optional.of(vehicle));

            // when & then
            assertThatThrownBy(() -> transactionService.requestTransaction(1L, seller))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("본인이 속한 회사의 차량은 구매할 수 없습니다.");
        }
    }

    // ==================== 거래 완료 테스트 ====================
    @Nested
    @DisplayName("거래 완료 (completeTransaction)")
    class CompleteTransaction {
        private Transaction transaction;

        @BeforeEach
        void setUpTransaction() {
            vehicle.changeStatus(VehicleStatus.RESERVED);

            transaction = Transaction.builder()
                    .transactionId(1L)
                    .buyer(buyer)
                    .company(company)
                    .vehicle(vehicle)
                    .price(new BigDecimal("25000000"))
                    .transactionStatus(TransactionStatus.APPROVED)
                    .build();
        }

        @Test
        @DisplayName("성공: 거래 완료 시 상태 변경 + 정산 생성")
        void 거래완료_성공() {
            // given
            given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));

            // when
            transactionService.completeTransaction(1L, seller);

            // then
            assertThat(transaction.getTransactionStatus()).isEqualTo(TransactionStatus.COMPLETED);
            assertThat(vehicle.getVehicleStatus()).isEqualTo(VehicleStatus.SOLD);
            verify(settlementRepository, times(1)).save(any(Settlement.class));
        }

        @Test
        @DisplayName("성공: 수수료 2.2% 계산")
        void 거래완료_수수료계산_검증() {
            // given
            given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));
            ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);

            // when
            transactionService.completeTransaction(1L, seller);

            // then
            verify(settlementRepository).save(captor.capture());
            Settlement saved = captor.getValue();

            // 2500만원 * 2.2% -> 55만원
            assertThat(saved.getCommissionAmount())
                    .isEqualByComparingTo(new BigDecimal("550000"));

            // 2500만원 - 55만원 -> 2445만원
            assertThat(saved.getSettlementAmount())
                    .isEqualByComparingTo(new BigDecimal("24450000"));
        }

        @Test
        @DisplayName("실패: APPROVED 상태가 아닌 거래")
        void 거래완료_실패_잘못된상태() {
            // given
            transaction.updateStatus(TransactionStatus.REQUESTED);
            given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));

            // when & then
            assertThatThrownBy(() -> transactionService.completeTransaction(1L, seller))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("진행 중(승인된) 거래만 완료");
        }

        @Test
        @DisplayName("실패: 권한 없음")
        void 거래완료_실패_권한없음() {
            // given
            User otherSeller = User.builder()
                    .userId(999L)
                    .email("other@test.com")
                    .name("다른판매자")
                    .password("password")
                    .phone("010-0000-0000")
                    .role(Role.COMPANY_OWNER)
                    .build();

            given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));

            // when & then
            assertThatThrownBy(() -> transactionService.completeTransaction(1L, otherSeller))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("권한이 없습니다");
        }
    }

    // ==================== 거래 승인 테스트 ====================
    @Nested
    @DisplayName("거래 승인 (approveTransaction)")
    class ApproveTransaction {

        private Transaction transaction;

        @BeforeEach
        void setUpTransaction() {
            transaction = Transaction.builder()
                    .transactionId(1L)
                    .buyer(buyer)
                    .company(company)
                    .vehicle(vehicle)
                    .price(vehicle.getPrice())
                    .transactionStatus(TransactionStatus.REQUESTED)
                    .build();
        }

        @Test
        @DisplayName("성공: 거래 승인")
        void 거래승인_성공() {
            // given
            given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));

            // when
            transactionService.approveTransaction(1L, seller);

            // then

            assertThat(transaction.getTransactionStatus()).isEqualTo(TransactionStatus.APPROVED);
            assertThat(vehicle.getVehicleStatus()).isEqualTo(VehicleStatus.RESERVED);
        }

        @Test
        @DisplayName("실패: 권한 없음")
        void 거래승인_실패_권한없음() {
            // given
            User otherSeller = User.builder()
                    .userId(999L)
                    .email("other@test.com")
                    .name("다른판매자")
                    .password("password")
                    .phone("010-0000-0000")
                    .role(Role.COMPANY_OWNER)
                    .build();

            given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));

            // when & then
            assertThatThrownBy(() -> transactionService.approveTransaction(1L, otherSeller))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("권한이 없습니다");
        }

        @Test
        @DisplayName("실패: REQUESTED 아닌 상태")
        void 거래승인_실패_잘못된상태() {
            // given
            transaction.updateStatus(TransactionStatus.APPROVED);
            given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));

            // when & then
            assertThatThrownBy(() -> transactionService.approveTransaction(1L, seller))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("대기 상태의 거래만 승인");
        }

        @Test
        @DisplayName("실패: 이미 예약된 차량")
        void 거래승인_실패_이미예약된차량() {
            // given
            vehicle.changeStatus(VehicleStatus.RESERVED);
            given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));

            // when & then
            assertThatThrownBy(() -> transactionService.approveTransaction(1L, seller))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 예약되었거나 판매된 차량");
        }
    }

    // ==================== 거래 거절 테스트 ====================
    @Nested
    @DisplayName("거래 거절 (rejectTransaction)")
    class RejectTransaction {

        private Transaction transaction;

        @BeforeEach
        void setUpTransaction() {
            transaction = Transaction.builder()
                    .transactionId(1L)
                    .buyer(buyer)
                    .company(company)
                    .vehicle(vehicle)
                    .price(vehicle.getPrice())
                    .transactionStatus(TransactionStatus.REQUESTED)
                    .build();
        }

        @Test
        @DisplayName("성공: 거래 거절")
        void 거래거절_성공() {
            // given
            given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));

            // when
            transactionService.rejectTransaction(1L, seller);

            // then
            assertThat(transaction.getTransactionStatus()).isEqualTo(TransactionStatus.REJECTED);
        }

        @Test
        @DisplayName("실패: 권한 없음")
        void 거래거절_실패_권한없음() {
            // given
            User otherSeller = User.builder()
                    .userId(999L)
                    .email("other@test.com")
                    .name("다른판매자")
                    .password("password")
                    .phone("010-0000-0000")
                    .role(Role.COMPANY_OWNER)
                    .build();

            given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));

            // when & then
            assertThatThrownBy(() -> transactionService.rejectTransaction(1L, otherSeller))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("권한이 없습니다");
        }

        @Test
        @DisplayName("실패: REQUESTED 아닌 상태")
        void 거래거절_실패_잘못된상태() {
            // given
            transaction.updateStatus(TransactionStatus.APPROVED);
            given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));

            // when & then
            assertThatThrownBy(() -> transactionService.rejectTransaction(1L, seller))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("대기 상태의 거래만 거부");
        }
    }

    // ==================== 거래 취소 테스트 ====================
    @Nested
    @DisplayName("거래 취소 (cancelTransaction)")
    class CancelTransaction {

        private Transaction transaction;

        @BeforeEach
        void setUpTransaction() {
            vehicle.changeStatus(VehicleStatus.RESERVED);

            transaction = Transaction.builder()
                    .transactionId(1L)
                    .buyer(buyer)
                    .company(company)
                    .vehicle(vehicle)
                    .price(vehicle.getPrice())
                    .transactionStatus(TransactionStatus.APPROVED)
                    .build();
        }

        @Test
        @DisplayName("성공: 구매자가 거래 취소 시 차량 다시 판매 상태로")
        void 거래취소_성공_구매자() {
            // Given
            given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));

            // When
            transactionService.cancelTransaction(1L, buyer);

            // Then
            assertThat(transaction.getTransactionStatus()).isEqualTo(TransactionStatus.CANCELLED);
            assertThat(vehicle.getVehicleStatus()).isEqualTo(VehicleStatus.SALE);
        }

        @Test
        @DisplayName("실패: 완료된 거래는 취소 불가")
        void 거래취소_실패_완료된거래() {
            // Given
            transaction.updateStatus(TransactionStatus.COMPLETED);

            given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));

            // When & Then
            assertThatThrownBy(() -> transactionService.cancelTransaction(1L, buyer))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("이미 완료된 거래는 취소할 수 없습니다.");
        }

        @Test
        @DisplayName("성공: 판매자가 거래 취소")
        void 거래취소_성공_판매자() {
            // given
            given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));

            // when
            transactionService.cancelTransaction(1L, seller);

            // then

            assertThat(transaction.getTransactionStatus()).isEqualTo(TransactionStatus.CANCELLED);
            assertThat(vehicle.getVehicleStatus()).isEqualTo(VehicleStatus.SALE);
        }

        @Test
        @DisplayName("실패: 권한 없음")
        void 거래취소_실패_권한없음() {
            // given
            User stranger = User.builder()
                    .userId(999L)
                    .email("stranger@test.com")
                    .name("제3자")
                    .password("password")
                    .phone("010-0000-0000")
                    .role(Role.CUSTOMER)
                    .build();

            given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));

            // when & then
            assertThatThrownBy(() -> transactionService.cancelTransaction(1L, stranger))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("거래 취소 권한이 없습니다.");
        }
    }
}