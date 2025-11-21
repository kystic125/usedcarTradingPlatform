package com.usedcar.trading.domain.user.entity;

import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.employee.entity.Employee;
import com.usedcar.trading.domain.favorite.entity.Favorite;
import com.usedcar.trading.domain.report.entity.Report;
import com.usedcar.trading.domain.review.entity.Review;
import com.usedcar.trading.domain.transaction.entity.Transaction;
import com.usedcar.trading.global.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA가 프록시 객체 생성할 때 필요
@AllArgsConstructor // @Builder가 내부적으로 사용할 전체 필드
@Builder // 빌더 패턴으로 객체 생성 가능
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String phone;

    @Enumerated(EnumType.STRING)
    private Provider provider = Provider.LOCAL;

    private String providerId;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private UserStatus userStatus = UserStatus.ACTIVE;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorite> favoriteList = new ArrayList<>();

    public void addFavorite(Favorite favorite) {
        this.favoriteList.add(favorite);
        favorite.setUser(this);
    }

    public void removeFavorite(Favorite favorite) {
        this.favoriteList.remove(favorite);
        favorite.setUser(null);
    }

    @OneToMany(mappedBy = "reporter")
    private List<Report> reportedReports = new ArrayList<>();

    @OneToMany(mappedBy = "handler")
    private List<Report> handledReports = new ArrayList<>();

    @OneToOne(mappedBy = "user")
    private Employee employee;

    @OneToOne(mappedBy = "owner")
    private Company company;

    /**
     * 관리자가 승인한 차 종류를 볼 필요가 있을까?
     */
    /*
    @OneToMany(mappedBy = "approved_by")
    private List<Vehicle> vehicles = new ArrayList<>();
    */

    @OneToMany(mappedBy = "buyer")
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Review> reviews = new ArrayList<>();
}