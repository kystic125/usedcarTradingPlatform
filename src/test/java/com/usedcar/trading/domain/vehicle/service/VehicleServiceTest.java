package com.usedcar.trading.domain.vehicle.service;

import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.domain.vehicle.entity.VehicleStatus;
import com.usedcar.trading.domain.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleService vehicleService;

    // 테스트용 공통 객체
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        // 차량 (판매 중)
        vehicle = Vehicle.builder()
                .vehicleId(1L)
                .brand("현대")
                .model("아반떼")
                .modelYear(2023)
                .mileage(10000)
                .price(new BigDecimal("25000000"))
                .vehicleStatus(VehicleStatus.SALE)
                .viewCount(0)
                .build();
    }

    // ==================== 조회수 증가 테스트 ====================
    @Nested
    @DisplayName("조회수 증가 (increaseViewCount)")
    class IncreaseViewCount {

        @Test
        @DisplayName("성공: 조회수 증가")
        void 조회수증가_성공() {
            // given
            given(vehicleRepository.findById(1L)).willReturn(Optional.of(vehicle));

            // when
            vehicleService.increaseViewCount(1L);

            // then
            assertThat(vehicle.getViewCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("성공: 조회수 여러 번 증가")
        void 조회수증가_여러번() {
            // given
            given(vehicleRepository.findById(1L)).willReturn(Optional.of(vehicle));

            // when
            vehicleService.increaseViewCount(1L);
            vehicleService.increaseViewCount(1L);
            vehicleService.increaseViewCount(1L);

            // then
            assertThat(vehicle.getViewCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 매물")
        void 조회수증가_실패_매물없음() {
            // given
            given(vehicleRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> vehicleService.increaseViewCount(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("매물 없음");
        }
    }

    // ==================== 매물 갱신 테스트 ====================
    @Nested
    @DisplayName("매물 갱신 (renewVehicle)")
    class RenewVehicle {

        @Test
        @DisplayName("성공: EXPIRED 매물 갱신")
        void 매물갱신_성공() {
            // given
            vehicle.changeStatus(VehicleStatus.EXPIRED);  // 만료 상태로 변경
            given(vehicleRepository.findById(1L)).willReturn(Optional.of(vehicle));

            // when
            vehicleService.renewVehicle(1L);

            // then
            assertThat(vehicle.getVehicleStatus()).isEqualTo(VehicleStatus.PENDING);
        }

        @Test
        @DisplayName("실패: EXPIRED 아닌 상태")
        void 매물갱신_실패_만료아님() {
            // given - SALE 상태 (만료 아님)
            given(vehicleRepository.findById(1L)).willReturn(Optional.of(vehicle));

            // when & then
            assertThatThrownBy(() -> vehicleService.renewVehicle(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("갱신 가능한 상태가 아닙니다");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 매물")
        void 매물갱신_실패_매물없음() {
            // given
            given(vehicleRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> vehicleService.renewVehicle(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("매물 없음");
        }
    }
}
