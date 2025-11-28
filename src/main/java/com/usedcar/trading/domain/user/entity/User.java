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
    @Builder.Default
    private Provider provider = Provider.LOCAL;

    private String providerId;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserStatus userStatus = UserStatus.ACTIVE;

    public void upgradeToOwner() {
        this.role = Role.COMPANY_OWNER;
    }

    public void linkSocial(Provider provider, String providerId) {
        //this.provider = provider;
        this.providerId = providerId;
    }

    public void unlinkSocial() {
        this.provider = Provider.LOCAL;
        this.providerId = null;
    }

    public void updateInfo(String email, String phone) {
        this.email = email;
        this.phone = phone;
    }

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
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
    @Builder.Default
    private List<Report> reportedReports = new ArrayList<>();

    @OneToMany(mappedBy = "handler")
    @Builder.Default
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
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    public void ban() {
        this.userStatus = UserStatus.BANNED;
    }
}