package com.usedcar.trading.domain.favorite.service;

import com.usedcar.trading.domain.favorite.entity.Favorite;
import com.usedcar.trading.domain.favorite.repository.FavoriteRepository;
import com.usedcar.trading.domain.user.entity.Role;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.domain.vehicle.entity.VehicleStatus;
import com.usedcar.trading.domain.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    /**
     * 찜 추가 [WISH-001]
     */
    @Transactional
    public Favorite addFavorite(Long userId, Long vehicleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (user.getRole() != Role.CUSTOMER) {
            throw new IllegalStateException("고객(구매자)만 이용 가능한 기능입니다.");
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("매물을 찾을 수 없습니다."));

        // 판매중인 매물만 찜 가능
        if (vehicle.getVehicleStatus() != VehicleStatus.SALE) {
            throw new IllegalStateException("판매중인 매물만 찜할 수 있습니다.");
        }

        // 이미 찜한 매물인지 확인
        if (favoriteRepository.existsByUserUserIdAndVehicleVehicleId(userId, vehicleId)) {
            throw new IllegalStateException("이미 찜한 매물입니다.");
        }

        Favorite favorite = Favorite.builder()
                .user(user)
                .vehicle(vehicle)
                .build();

        Favorite savedFavorite = favoriteRepository.save(favorite);
        log.info("찜 추가: userId={}, vehicleId={}", userId, vehicleId);

        return savedFavorite;
    }

    /**
     * 찜 삭제 [WISH-002]
     */
    @Transactional
    public void removeFavorite(Long userId, Long vehicleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (user.getRole() != Role.CUSTOMER) {
            throw new IllegalStateException("권한이 없습니다.");
        }

        Favorite favorite = favoriteRepository.findByUserUserIdAndVehicleVehicleId(userId, vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("찜한 매물을 찾을 수 없습니다."));

        favoriteRepository.delete(favorite);
        log.info("찜 삭제: userId={}, vehicleId={}", userId, vehicleId);
    }

    /**
     * 내 찜 목록 조회 [WISH-003]
     */
    public List<Favorite> getMyFavorites(Long userId) {
        return favoriteRepository.findByUserUserId(userId);
    }

    /**
     * 찜 여부 확인
     */
    public boolean isFavorite(Long userId, Long vehicleId) {
        return favoriteRepository.existsByUserUserIdAndVehicleVehicleId(userId, vehicleId);
    }

    /**
     * 매물 찜 개수 조회
     */
    public int getFavoriteCount(Long vehicleId) {
        return favoriteRepository.countByVehicleVehicleId(vehicleId);
    }

    /**
     * 내 찜 개수 조회
     */
    public int getMyFavoriteCount(Long userId) {
        return favoriteRepository.countByUserUserId(userId);
    }
}
