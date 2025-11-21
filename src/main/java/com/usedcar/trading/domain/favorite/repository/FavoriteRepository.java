package com.usedcar.trading.domain.favorite.repository;

import com.usedcar.trading.domain.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    // 특정 유저의 찜 목록 조회
    List<Favorite> findByUserUserId(Long userId);

    // 특정 차량을 찜한 사람 목록 조회
    List<Favorite> findByVehicleVehicleId(Long vehicleId);

    // 특정 유저가 특정 차량을 찜했는지 확인 (중복 체크)
    boolean existsByUserUserIdAndVehicleVehicleId(Long userId, Long vehicleId);

    // 특정 유저의 특정 차량 찜 조회
    Optional<Favorite> findByUserUserIdAndVehicleVehicleId(Long userId, Long vehicleId);

    // 특정 유저의 찜 개수 조회
    int countByUserUserId(Long userId);

    // 특정 차량의 찜 개수 조회
    int countByVehicleVehicleId(Long vehicleId);
}
