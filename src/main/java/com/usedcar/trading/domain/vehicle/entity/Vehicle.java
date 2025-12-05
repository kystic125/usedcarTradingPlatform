package com.usedcar.trading.domain.vehicle.entity;

import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.employee.entity.Employee;
import com.usedcar.trading.domain.favorite.entity.Favorite;
import com.usedcar.trading.domain.transaction.entity.Transaction;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.global.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Vehicle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long vehicleId;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private int modelYear;

    @Column(nullable = false)
    private int mileage;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FuelType fuelType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Transmission transmission;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private VehicleStatus vehicleStatus;

    private String color;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    private Boolean accidentHistory = false;

    private String options;

    @Builder.Default
    private int viewCount = 0;

    private String thumbnailUrl;

    private LocalDateTime approvedAt;

    @Column(columnDefinition = "TEXT")
    private String rejectedReason;

    /**
     * Vehicle : VehicleImage => 1 : n
     */
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VehicleImage> images = new ArrayList<>();

    // 연관 관계 편의 메서드
    public void addImage(VehicleImage image) {
        this.images.add(image);
        image.setVehicle(this);
    }

    public void removeImage(VehicleImage image) {
        this.images.remove(image);
        image.setVehicle(null);
    }


    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Favorite> favorites = new ArrayList<>();

    // 연관관계 편의 메서드
    public void addFavorite(Favorite favorite) {
        this.favorites.add(favorite);
        favorite.setVehicle(this);
    }

    public void removeFavorite(Favorite favorite) {
        this.favorites.remove(favorite);
        favorite.setVehicle(null);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    public void setCompany(Company company) {
        this.company = company;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by", nullable = false)
    private Employee registeredBy;

    public void setEmployee(Employee employee) {
        this.registeredBy = employee;
    }

    public void approve(User admin) {
        this.vehicleStatus = VehicleStatus.SALE;
        this.approvedAt = LocalDateTime.now();
        this.approvedBy = admin;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by") //, nullable = false)
    private User approvedBy;

    @OneToMany(mappedBy = "vehicle")
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    public void reject(String reason, User admin) {
        this.vehicleStatus = VehicleStatus.REJECTED;
        this.rejectedReason = reason;
        this.approvedBy = admin;
        this.approvedAt = LocalDateTime.now();
    }

    public void changeStatus(VehicleStatus status) {
        this.vehicleStatus = status;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    private LocalDateTime expirationDate;

    public void extendExpirationDate() {
        this.expirationDate = LocalDateTime.now().plusDays(14);
    }

    public void forceDeleteByAdmin() {
        this.vehicleStatus = VehicleStatus.DELETED;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void updateVehicleInfo(String model, int modelYear, int mileage, BigDecimal price,
                                  String description, String options) {
        this.model = model;
        this.modelYear = modelYear;
        this.mileage = mileage;
        this.price = price;
        this.description = description;
        this.options = options;
    }

    public void setRegisteredBy(Employee employee) {
        this.registeredBy = employee;
    }
}