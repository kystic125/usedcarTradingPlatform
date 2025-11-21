package com.usedcar.trading.domain.vehicle.repository;

import com.usedcar.trading.domain.vehicle.entity.VehicleImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleImageRepository extends JpaRepository<VehicleImage, Long> {

    List<VehicleImage> findByVehicleVehicleIdOrderByDisplayOrderAsc(Long vehicleId);
}
