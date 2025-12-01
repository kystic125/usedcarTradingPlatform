package com.usedcar.trading.domain.favorite.service;

import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.favorite.entity.Favorite;
import com.usedcar.trading.domain.favorite.repository.FavoriteRepository;
import com.usedcar.trading.domain.user.entity.Role;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    // 테스트용 공통 객체
    private User user;
    private Vehicle vehicle;
    private Favorite favorite;

    @BeforeEach
    void setUp() {
        // 사용자
        user = User.builder()
                .userId(1L)
                .email("user@test.com")
                .name("테스트유저")
                .password("password")
                .phone("010-1234-5678")
                .role(Role.CUSTOMER)
                .build();

        // 차량 (판매 중)
        vehicle = Vehicle.builder()
                .vehicleId(1L)
                .brand("현대")
                .model("아반떼")
                .modelYear(2023)
                .price(new BigDecimal("25000000"))
                .vehicleStatus(VehicleStatus.SALE)
                .build();

        // 찜
        favorite = Favorite.builder()
                .favoriteId(1L)
                .user(user)
                .vehicle(vehicle)
                .build();
    }

    // ==================== 찜 추가 테스트 ====================
    @Nested
    @DisplayName("찜 추가 (addFavorite)")
    class AddFavorite {

        @Test
        @DisplayName("성공: 판매 중인 매물 찜 추가")
        void 찜추가_성공() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(vehicleRepository.findById(1L)).willReturn(Optional.of(vehicle));
            given(favoriteRepository.existsByUserUserIdAndVehicleVehicleId(1L, 1L)).willReturn(false);
            given(favoriteRepository.save(any(Favorite.class)))
                    .willAnswer(invocation -> {
                        Favorite saved = invocation.getArgument(0);
                        return Favorite.builder()
                                .favoriteId(100L)
                                .user(saved.getUser())
                                .vehicle(saved.getVehicle())
                                .build();
                    });

            // when
            Favorite result = favoriteService.addFavorite(1L, 1L);

            // then
            assertThat(result.getFavoriteId()).isEqualTo(100L);
            verify(favoriteRepository, times(1)).save(any(Favorite.class));
        }

        @Test
        @DisplayName("실패: 판매 중이 아닌 매물")
        void 찜추가_실패_판매중아님() {
            // given
            vehicle.changeStatus(VehicleStatus.RESERVED);  // 예약됨
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(vehicleRepository.findById(1L)).willReturn(Optional.of(vehicle));

            // when & then
            assertThatThrownBy(() -> favoriteService.addFavorite(1L, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("판매중인 매물만 찜할 수 있습니다");
        }

        @Test
        @DisplayName("실패: 이미 찜한 매물")
        void 찜추가_실패_중복찜() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(vehicleRepository.findById(1L)).willReturn(Optional.of(vehicle));
            given(favoriteRepository.existsByUserUserIdAndVehicleVehicleId(1L, 1L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> favoriteService.addFavorite(1L, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 찜한 매물입니다");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자")
        void 찜추가_실패_사용자없음() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> favoriteService.addFavorite(999L, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 매물")
        void 찜추가_실패_매물없음() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(vehicleRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> favoriteService.addFavorite(1L, 999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("매물을 찾을 수 없습니다");
        }
    }

    // ==================== 찜 삭제 테스트 ====================
    @Nested
    @DisplayName("찜 삭제 (removeFavorite)")
    class RemoveFavorite {

        @Test
        @DisplayName("성공: 찜 삭제")
        void 찜삭제_성공() {
            // given
            given(favoriteRepository.findByUserUserIdAndVehicleVehicleId(1L, 1L))
                    .willReturn(Optional.of(favorite));

            // when
            favoriteService.removeFavorite(1L, 1L);

            // then
            verify(favoriteRepository, times(1)).delete(favorite);
        }

        @Test
        @DisplayName("실패: 찜하지 않은 매물")
        void 찜삭제_실패_찜안함() {
            // given
            given(favoriteRepository.findByUserUserIdAndVehicleVehicleId(1L, 999L))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> favoriteService.removeFavorite(1L, 999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("찜한 매물을 찾을 수 없습니다");
        }
    }

    // ==================== 내 찜 목록 조회 테스트 ====================
    @Nested
    @DisplayName("내 찜 목록 조회 (getMyFavorites)")
    class GetMyFavorites {

        @Test
        @DisplayName("성공: 찜 목록 조회")
        void 찜목록_성공() {
            // given
            Favorite favorite2 = Favorite.builder()
                    .favoriteId(2L)
                    .user(user)
                    .build();

            given(favoriteRepository.findByUserUserId(1L))
                    .willReturn(Arrays.asList(favorite, favorite2));

            // when
            List<Favorite> result = favoriteService.getMyFavorites(1L);

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("성공: 찜 목록 없음")
        void 찜목록_없음() {
            // given
            given(favoriteRepository.findByUserUserId(1L))
                    .willReturn(Arrays.asList());

            // when
            List<Favorite> result = favoriteService.getMyFavorites(1L);

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== 찜 여부 확인 테스트 ====================
    @Nested
    @DisplayName("찜 여부 확인 (isFavorite)")
    class IsFavorite {

        @Test
        @DisplayName("성공: 찜한 매물")
        void 찜여부_찜함() {
            // given
            given(favoriteRepository.existsByUserUserIdAndVehicleVehicleId(1L, 1L))
                    .willReturn(true);

            // when
            boolean result = favoriteService.isFavorite(1L, 1L);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("성공: 찜하지 않은 매물")
        void 찜여부_안찜함() {
            // given
            given(favoriteRepository.existsByUserUserIdAndVehicleVehicleId(1L, 1L))
                    .willReturn(false);

            // when
            boolean result = favoriteService.isFavorite(1L, 1L);

            // then
            assertThat(result).isFalse();
        }
    }

    // ==================== 찜 개수 조회 테스트 ====================
    @Nested
    @DisplayName("찜 개수 조회 (getFavoriteCount)")
    class GetFavoriteCount {

        @Test
        @DisplayName("성공: 매물 찜 개수 조회")
        void 매물찜개수_성공() {
            // given
            given(favoriteRepository.countByVehicleVehicleId(1L)).willReturn(10);

            // when
            int result = favoriteService.getFavoriteCount(1L);

            // then
            assertThat(result).isEqualTo(10);
        }

        @Test
        @DisplayName("성공: 내 찜 개수 조회")
        void 내찜개수_성공() {
            // given
            given(favoriteRepository.countByUserUserId(1L)).willReturn(5);

            // when
            int result = favoriteService.getMyFavoriteCount(1L);

            // then
            assertThat(result).isEqualTo(5);
        }
    }
}
