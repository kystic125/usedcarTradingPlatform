package com.usedcar.trading.domain.company.entity;

import com.usedcar.trading.domain.employee.entity.Employee;
import com.usedcar.trading.domain.review.entity.Review;
import com.usedcar.trading.domain.settlement.entity.Settlement;
import com.usedcar.trading.domain.transaction.entity.Transaction;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.global.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long companyId;

    @Column(nullable = false)
    private String businessName;

    @Column(unique = true, nullable = false)
    private String businessNumber;

    @Column(nullable = false)
    private String address;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String logoUrl;

    private String bankAccount;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CompanyStatus companyStatus = CompanyStatus.ACTIVE;

    @Builder.Default
    private BigDecimal averageRating = new BigDecimal("0.0");

    @Builder.Default
    private int totalReviews = 0;

    @Builder.Default
    private int totalSales = 0;

    // int 형으로 관리할건지 확인하기
    @Builder.Default
    private int trustScore = 0;

    @OneToMany(mappedBy = "company")
    @Builder.Default
    private List<Employee> employees = new ArrayList<>();

    // 연관관계 편의 메서드
    public void addEmployee(Employee employee) {
        this.employees.add(employee);
        employee.setCompany(this);
    }

    public void removeEmployee(Employee employee) {
        this.employees.remove(employee);
        employee.setCompany(null);
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "company")
    @Builder.Default
    private List<Vehicle> vehicles = new ArrayList<>();

    // 연관관계 편의 메서드
    public void addVehicle(Vehicle vehicle) {
        this.vehicles.add(vehicle);
        vehicle.setCompany(this);
    }

    public void removeVehicle(Vehicle vehicle) {
        this.vehicles.remove(vehicle);
        vehicle.setCompany(null);
    }

    @OneToMany(mappedBy = "company")
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "company")
    @Builder.Default
    private List<Settlement> settlements = new ArrayList<>();

    @OneToMany(mappedBy = "company")
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();
}
