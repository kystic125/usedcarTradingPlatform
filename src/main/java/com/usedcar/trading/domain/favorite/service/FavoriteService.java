package com.usedcar.trading.domain.favorite.service;

import com.usedcar.trading.domain.favorite.entity.Favorite;
import com.usedcar.trading.domain.favorite.repository.FavoriteRepository;
import com.usedcar.trading.domain.user.entity.Role;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.domain.vehicle.entity.VehicleStatus;
import com.usedcar.trading.domain.vehicle.repository.VehicleRepository;
import com.usedcar.trading.global.exception.CustomException;
import com.usedcar.trading.global.exception.ErrorCode;
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
    
    // 찜 추가
    @Transactional
    public void addFavorite(Long userId, Long vehicleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() != Role.CUSTOMER) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new CustomException(ErrorCode.VEHICLE_NOT_FOUND));

        if (vehicle.getVehicleStatus() != VehicleStatus.SALE) {
            throw new CustomException(ErrorCode.VEHICLE_NOT_FOR_SALE);
        }

        if (favoriteRepository.existsByUserUserIdAndVehicleVehicleId(userId, vehicleId)) {
            throw new CustomException(ErrorCode.ALREADY_FAVORITED);
        }

        Favorite favorite = Favorite.builder()
                .user(user)
                .vehicle(vehicle)
                .build();

        favoriteRepository.save(favorite);
        log.info("찜 추가: userId={}, vehicleId={}", userId, vehicleId);
    }
    
    // 찜 삭제
    @Transactional
    public void removeFavorite(Long userId, Long vehicleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() != Role.CUSTOMER) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        Favorite favorite = favoriteRepository.findByUserUserIdAndVehicleVehicleId(userId, vehicleId)
                .orElseThrow(() -> new CustomException(ErrorCode.FAVORITE_NOT_FOUND));

        favoriteRepository.delete(favorite);
        log.info("찜 삭제: userId={}, vehicleId={}", userId, vehicleId);
    }

    // 내 찜 목록 조회
    public List<Favorite> getMyFavorites(Long userId) {
        return favoriteRepository.findByUserUserId(userId);
    }


    // 찜 여부 확인
    public boolean isFavorite(Long userId, Long vehicleId) {
        return favoriteRepository.existsByUserUserIdAndVehicleVehicleId(userId, vehicleId);
    }


    // 매물 찜 개수 조회
    public int getFavoriteCount(Long vehicleId) {
        return favoriteRepository.countByVehicleVehicleId(vehicleId);
    }


    // 내 찜 개수 조회
    public int getMyFavoriteCount(Long userId) {
        return favoriteRepository.countByUserUserId(userId);
    }
}
