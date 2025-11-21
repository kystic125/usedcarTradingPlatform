package com.usedcar.trading.domain.employee.entity;

import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.global.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Employee extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    private EmployeePosition employeePosition;

    private Boolean isActive = true;

    private LocalDateTime joinedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    public void setCompany(Company company) {
        this.company = company;
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @OneToMany(mappedBy = "registeredBy")
    private List<Vehicle> vehicles = new ArrayList<>();

    // 연관관계 편의 메서드
    public void addVehicle(Vehicle vehicle) {
        this.vehicles.add(vehicle);
        vehicle.setEmployee(this);
    }

    public void removeVehicle(Vehicle vehicle) {
        this.vehicles.remove(vehicle);
        vehicle.setEmployee(null);
    }
}
