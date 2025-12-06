package com.usedcar.trading.global.config;

import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.company.entity.CompanyStatus;
import com.usedcar.trading.domain.company.repository.CompanyRepository;
import com.usedcar.trading.domain.employee.entity.Employee;
import com.usedcar.trading.domain.employee.entity.EmployeePosition;
import com.usedcar.trading.domain.employee.repository.EmployeeRepository;
import com.usedcar.trading.domain.review.entity.Review;
import com.usedcar.trading.domain.review.repository.ReviewRepository;
import com.usedcar.trading.domain.transaction.entity.Transaction;
import com.usedcar.trading.domain.transaction.entity.TransactionStatus;
import com.usedcar.trading.domain.transaction.repository.TransactionRepository;
import com.usedcar.trading.domain.user.entity.Provider;
import com.usedcar.trading.domain.user.entity.Role;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.entity.UserStatus;
import com.usedcar.trading.domain.user.repository.UserRepository;
import com.usedcar.trading.domain.vehicle.entity.FuelType;
import com.usedcar.trading.domain.vehicle.entity.Transmission;
import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.domain.vehicle.entity.VehicleStatus;
import com.usedcar.trading.domain.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInit implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final VehicleRepository vehicleRepository;
    private final TransactionRepository transactionRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        if (userRepository.existsByEmail("admin@smartpick.com")) {
            return;
        }

        // ==========================================
        // 1. 계정 생성 (Admin, Boss, Staff, Buyer)
        // ==========================================

        // 1-1. 관리자
        User admin = createUser("admin@smartpick.com", "admin1234", "총관리자", Role.ADMIN);

        // 1-2. 사장님 (Smart Motors)
        User boss = createUser("boss@test.com", "1234", "김사장", Role.COMPANY_OWNER);

        // 1-3. 직원 (Smart Motors 소속)
        User staff = createUser("staff@test.com", "1234", "이직원", Role.COMPANY_EMPLOYEE);

        // 1-4. 구매자
        User buyer = createUser("buyer@test.com", "1234", "박구매", Role.CUSTOMER);

        System.out.println(">>> 계정 생성 완료");


        // ==========================================
        // 2. 회사 및 직원 연결
        // ==========================================

        Company company = companyRepository.save(Company.builder()
                .owner(boss)
                .businessName("스마트 모터스")
                .businessNumber("123-45-67890")
                .address("서울시 강남구 테헤란로 123")
                .companyStatus(CompanyStatus.ACTIVE)
                .build());

        // 사장님을 직원으로 등록 (LEADER)
        employeeRepository.save(Employee.builder()
                .user(boss)
                .company(company)
                .employeePosition(EmployeePosition.LEADER)
                .isActive(true)
                .build());

        // 직원을 회사에 소속시킴 (DEALER)
        Employee dealer = employeeRepository.save(Employee.builder()
                .user(staff)
                .company(company)
                .employeePosition(EmployeePosition.DEALER)
                .isActive(true)
                .build());

        System.out.println(">>> 회사 및 직원 연결 완료");


        // ==========================================
        // 3. 매물 데이터 생성 (다양한 상태)
        // ==========================================

        // 3-1. [SALE] 판매 중인 차량 (메인화면, 검색 테스트용) - 4대
        createVehicle(dealer, company, "현대", "그랜저 GN7", 2023, 15000, FuelType.GASOLINE, 42000000, VehicleStatus.SALE, "블랙", admin);
        createVehicle(dealer, company, "기아", "쏘렌토 MQ4", 2022, 32000, FuelType.DIESEL, 38000000, VehicleStatus.SALE, "화이트", admin);
        createVehicle(dealer, company, "제네시스", "G80", 2021, 45000, FuelType.GASOLINE, 55000000, VehicleStatus.SALE, "그레이", admin);
        createVehicle(dealer, company, "벤츠", "E-Class", 2020, 60000, FuelType.GASOLINE, 62000000, VehicleStatus.SALE, "실버", admin);

        // 3-2. [PENDING] 승인 대기 차량 (관리자 승인/반려 테스트용)
        createVehicle(dealer, company, "BMW", "520i", 2024, 5000, FuelType.GASOLINE, 70000000, VehicleStatus.PENDING, "블루", null);

        // 3-3. [REJECTED] 반려된 차량 (판매자 수정 테스트용)
        Vehicle rejectedCar = createVehicle(dealer, company, "아우디", "A6", 2019, 80000, FuelType.DIESEL, 30000000, VehicleStatus.REJECTED, "화이트", admin);
        rejectedCar.reject("가격 정보가 시세와 맞지 않습니다.", admin); // 반려 사유 입력

        // 3-4. [SOLD] 판매 완료 차량 (거래 내역 및 리뷰 테스트용)
        Vehicle soldCar = createVehicle(dealer, company, "포르쉐", "카이엔", 2018, 90000, FuelType.GASOLINE, 95000000, VehicleStatus.SOLD, "블랙", admin);

        System.out.println(">>> 매물 데이터 생성 완료");


        // ==========================================
        // 4. 거래 및 리뷰 데이터 생성
        // ==========================================

        // 4-1. 완료된 거래 (구매자가 '포르쉐 카이엔'을 구매함)
        Transaction transaction = transactionRepository.save(Transaction.builder()
                .buyer(buyer)
                .company(company)
                .vehicle(soldCar)
                .price(soldCar.getPrice())
                .transactionStatus(TransactionStatus.COMPLETED) // 거래 완료
                .requestedAt(LocalDateTime.now().minusDays(10))
                .approvedAt(LocalDateTime.now().minusDays(9))
                .completedAt(LocalDateTime.now().minusDays(5))
                .build());

        // 4-2. 작성된 리뷰 (이 거래에 대한 리뷰)
        reviewRepository.save(Review.builder()
                .user(buyer)
                .company(company)
                .transaction(transaction)
                .rating(5)
                .content("딜러님이 정말 친절하고 차 상태도 완벽합니다! 강력 추천해요.")
                .build());

        System.out.println(">>> 거래 및 리뷰 데이터 생성 완료");
    }

    // 유저 생성 헬퍼 메서드
    private User createUser(String email, String password, String name, Role role) {
        return userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name(name)
                .phone("010-1234-5678")
                .role(role)
                .provider(Provider.LOCAL)
                .userStatus(UserStatus.ACTIVE)
                .build());
    }

    // 차량 생성 헬퍼 메서드
    private Vehicle createVehicle(Employee dealer, Company company, String brand, String model, int year, int mileage, FuelType fuel, int price, VehicleStatus status, String color, User admin) {
        return vehicleRepository.save(Vehicle.builder()
                .registeredBy(dealer)
                .company(company)
                .brand(brand)
                .model(model)
                .modelYear(year)
                .mileage(mileage)
                .fuelType(fuel)
                .transmission(Transmission.AUTO)
                .price(new BigDecimal(price))
                .vehicleStatus(status)
                .color(color)
                .description("테스트용 차량입니다. 상태: " + status)
                .approvedBy(status == VehicleStatus.SALE || status == VehicleStatus.SOLD ? admin : null)
                .approvedAt(status == VehicleStatus.SALE || status == VehicleStatus.SOLD ? LocalDateTime.now() : null)
                .viewCount(0)
                .build());
    }
}