package com.usedcar.trading.domain.vehicle.repository;

import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.employee.entity.Employee;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.vehicle.entity.FuelType;
import com.usedcar.trading.domain.vehicle.entity.Transmission;
import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.domain.vehicle.entity.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    /**
     * 검색 기준
     */
    // 상태별
    List<Vehicle> findByVehicleStatus(VehicleStatus status);

    // 업체별
    List<Vehicle> findByCompany(Company company);

    // 브랜드별
    List<Vehicle> findByBrand(String brand);

    // 모델별
    List<Vehicle> findByModel(String model);

    // 브랜드 + 모델 조합
    List<Vehicle> findByBrandAndModel(String brand, String model);

    // 직원이 등록한 매물
    List<Vehicle> findByRegisteredBy(Employee employee);

    // 승인자별 // 필요 없어보임
    List<Vehicle> findByApprovedBy(User approvedBy);

    // 업체 + 상태 조합
    List<Vehicle> findByCompanyAndVehicleStatus(Company company, VehicleStatus status);

    /**
     * 범위 조회
     */
    // 가격대 조회
    List<Vehicle> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // 연식 범위
    List<Vehicle> findByModelYearBetween(int startYear, int endYear);

    // 주행거리 범위
    List<Vehicle> findByMileageLessThanEqual(int maxMileage);

    /**
     * 필터링
     */
    // 연료 타입별
    List<Vehicle> findByFuelType(FuelType fuelType);

    // 변속기별
    List<Vehicle> findByTransmission(Transmission transmission);

    // 사고 이력 여부
    List<Vehicle> findByAccidentHistory(Boolean hasAccident);

    // 상태 + 사고이력 조합
    List<Vehicle> findByVehicleStatusAndAccidentHistory(VehicleStatus status, Boolean hasAccident);

    /**
     * 정렬
     */
    // 최신 등록순
    List<Vehicle> findByVehicleStatusOrderByCreatedAtDesc(VehicleStatus status);

    // 가격 낮은순
    List<Vehicle> findByVehicleStatusOrderByPriceAsc(VehicleStatus status);

    // 가격 높은순
    List<Vehicle> findByVehicleStatusOrderByPriceDesc(VehicleStatus status);

    // 주행거리 낮은순
    List<Vehicle> findByVehicleStatusOrderByMileageAsc(VehicleStatus status);

    // 인기순 (조회수 높은순)
    List<Vehicle> findByVehicleStatusOrderByViewCountDesc(VehicleStatus status);

    /**
     * 개수
     */
    // 상태별 개수
    long countByVehicleStatus(VehicleStatus status);

    // 브랜드별 개수
    long countByBrand(String brand);

    // 업체별 매물 개수
    long countByCompany(Company company);

    // 업체 + 상태별 개수
    long countByCompanyAndVehicleStatus(Company company, VehicleStatus status);

    /**
     * 그외 복합 검색 조건
     */
    // 복합 검색: 브랜드 + 가격대 + 연식 + 주행거리 등
    @Query("SELECT v FROM Vehicle v WHERE " +
            "v.vehicleStatus = 'SALE' AND " +
            "(:keyword IS NULL OR LOWER(v.brand) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(v.model) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:minPrice IS NULL OR v.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR v.price <= :maxPrice) AND " +
            "(:minYear IS NULL OR v.modelYear >= :minYear) AND " +
            "(:maxYear IS NULL OR v.modelYear <= :maxYear) AND " +
            "(:maxMileage IS NULL OR v.mileage <= :maxMileage) AND " +
            "(:fuelTypes IS NULL OR v.fuelType IN :fuelTypes) AND " +
            "(:transmissions IS NULL OR v.transmission IN :transmissions)")
    List<Vehicle> searchVehicles(
            @Param("keyword") String keyword,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minYear") Integer minYear,
            @Param("maxYear") Integer maxYear,
            @Param("maxMileage") Integer maxMileage,
            @Param("fuelTypes") List<FuelType> fuelTypes,
            @Param("transmissions") List<Transmission> transmissions
    );

    // 복합 검색 + 정렬 (가격순)
    @Query("SELECT v FROM Vehicle v WHERE " +
            "v.vehicleStatus = :status AND " +
            "(:brand IS NULL OR v.brand = :brand) AND " +
            "v.price BETWEEN :minPrice AND :maxPrice " +
            "ORDER BY v.price ASC")
    List<Vehicle> searchVehiclesByPriceAsc(
            @Param("status") VehicleStatus status,
            @Param("brand") String brand,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );

    /**
     * 관리자 확인용
     */
    // 승인 대기 매물
    List<Vehicle> findByVehicleStatusOrderByCreatedAtAsc(VehicleStatus status);
    // PENDING

    // 반려 사유 있는 매물
    @Query("SELECT v FROM Vehicle v WHERE v.vehicleStatus = 'REJECTED' AND v.rejectedReason IS NOT NULL")
    List<Vehicle> findRejectedVehiclesWithReason();

    // 승인되지 않은 매물 개수
    long countByVehicleStatusIn(List<VehicleStatus> statuses); // [PENDING, REJECTED]

    List<Vehicle> findByVehicleStatusAndExpirationDateBetween(VehicleStatus status, LocalDateTime start, LocalDateTime end);
    List<Vehicle> findByVehicleStatusAndExpirationDateBefore(VehicleStatus status, LocalDateTime date);

}
