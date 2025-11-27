package com.usedcar.trading.global.config;

import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.company.entity.CompanyStatus;
import com.usedcar.trading.domain.company.repository.CompanyRepository;
import com.usedcar.trading.domain.employee.entity.Employee;
import com.usedcar.trading.domain.employee.entity.EmployeePosition;
import com.usedcar.trading.domain.employee.repository.EmployeeRepository;
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
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        // 1. 관리자 (ADMIN) 계정 생성
        if (!userRepository.existsByEmail("admin@smartpick.com")) {
            userRepository.save(User.builder()
                    .email("admin@smartpick.com")
                    .password(passwordEncoder.encode("admin1234")) // 비번: admin1234
                    .name("총관리자")
                    .phone("010-0000-0000")
                    .role(Role.ADMIN) // [핵심] ADMIN 권한
                    .provider(Provider.LOCAL)
                    .userStatus(UserStatus.ACTIVE)
                    .build());
            System.out.println("관리자 계정 생성 완료: admin@smartpick.com / admin1234");
        }

        // 2. 테스트용 사장님 & 회사 & 직원 세트 생성
        if (!userRepository.existsByEmail("boss@test.com")) {
            // 사장님
            User boss = userRepository.save(User.builder()
                    .email("boss@test.com")
                    .password(passwordEncoder.encode("1234"))
                    .name("김사장")
                    .phone("010-1111-1111")
                    .role(Role.COMPANY_OWNER)
                    .provider(Provider.LOCAL)
                    .userStatus(UserStatus.ACTIVE)
                    .build());

            // 회사
            Company company = companyRepository.save(Company.builder()
                    .owner(boss)
                    .businessName("스마트 중고차")
                    .businessNumber("123-45-67890")
                    .address("서울시 강남구")
                    .companyStatus(CompanyStatus.ACTIVE)
                    .build());

            employeeRepository.save(Employee.builder()
                    .user(boss)
                    .company(company)
                    .employeePosition(EmployeePosition.LEADER)
                    .isActive(true)
                    .build());

            // 직원 (로그인용)
            User staffUser = userRepository.save(User.builder()
                    .email("staff@test.com")
                    .password(passwordEncoder.encode("1234"))
                    .name("이직원")
                    .phone("010-2222-2222")
                    .role(Role.COMPANY_EMPLOYEE)
                    .provider(Provider.LOCAL)
                    .userStatus(UserStatus.ACTIVE)
                    .build());

            // 직원 (소속 연결)
            employeeRepository.save(Employee.builder()
                    .user(staffUser)
                    .company(company)
                    .employeePosition(EmployeePosition.DEALER)
                    .isActive(true)
                    .build());

            System.out.println("테스트용 사장/직원 계정 생성 완료");
        }

        // 3. 관리자 테스트용 가짜 매물 생성
        if (vehicleRepository.count() == 0) {

            User staffUser = userRepository.findByEmail("staff@test.com").orElse(null);

            if (staffUser != null) {
                Employee dealer = employeeRepository.findByUserUserId(staffUser.getUserId()).orElseThrow();
                Company company = dealer.getCompany();

                Vehicle pendingCar = Vehicle.builder()
                        .company(company)
                        .registeredBy(dealer)
                        .brand("현대")
                        .model("그랜저")
                        .modelYear(2024)
                        .mileage(5000)
                        .fuelType(FuelType.GASOLINE)
                        .transmission(Transmission.AUTO)
                        .price(new BigDecimal("35000000"))
                        .vehicleStatus(VehicleStatus.PENDING)
                        .color("화이트")
                        .description("관리자가 승인해야 하는 테스트 차량입니다.")
                        .build();

                vehicleRepository.save(pendingCar);
                System.out.println("테스트용 '승인 대기(PENDING)' 차량 생성 완료");
            }
        }

        // 4. 테스트용 '판매 중(SALE)' 차량 생성
        if (vehicleRepository.countByVehicleStatus(VehicleStatus.SALE) == 0) {
            User staffUser = userRepository.findByEmail("staff@test.com").orElse(null);
            if (staffUser != null) {
                Employee dealer = employeeRepository.findByUserUserId(staffUser.getUserId()).orElseThrow();

                Vehicle saleCar = Vehicle.builder()
                        .company(dealer.getCompany())
                        .registeredBy(dealer)
                        .brand("기아")
                        .model("쏘렌토")
                        .modelYear(2023)
                        .mileage(12000)
                        .fuelType(FuelType.DIESEL)
                        .transmission(Transmission.AUTO)
                        .price(new BigDecimal("42000000"))
                        .vehicleStatus(VehicleStatus.SALE)
                        .color("블랙")
                        .description("관리자가 이미 승인한, 즉시 구매 가능한 차량입니다.")
                        .approvedBy(userRepository.findByEmail("admin@smartpick.com").orElse(null))
                        .approvedAt(LocalDateTime.now())
                        .build();

                vehicleRepository.save(saleCar);
                System.out.println("테스트용 '판매 중(SALE)' 차량 생성 완료: 기아 쏘렌토");
            }
        }

        // 5. 테스트용 '구매자' 계정 생성
        if (!userRepository.existsByEmail("buyer@test.com")) {
            userRepository.save(User.builder()
                    .email("buyer@test.com")
                    .password(passwordEncoder.encode("1234"))
                    .name("김구매")
                    .phone("010-9999-9999")
                    .role(Role.CUSTOMER)
                    .provider(Provider.LOCAL)
                    .userStatus(UserStatus.ACTIVE)
                    .build());
            System.out.println("테스트용 구매자 계정 생성 완료: buyer@test.com / 1234");
        }
    }
}