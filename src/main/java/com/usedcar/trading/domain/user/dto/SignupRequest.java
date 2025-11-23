package com.usedcar.trading.domain.user.dto;

import com.usedcar.trading.domain.user.entity.Provider;
import com.usedcar.trading.domain.user.entity.Role;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.entity.UserStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
    private String email;
    private String password;
    private String name;
    private String phone;

    private String role;

    private String businessName;
    private String businessNumber;
    private String address;

    public User toEntity(String encodedPassword) {
        Role userRole = "OWNER".equals(this.role) ? Role.COMPANY_OWNER : Role.CUSTOMER;

        return User.builder()
                .email(this.email)
                .password(encodedPassword)
                .name(this.name)
                .phone(this.phone)
                .role(userRole)
                .userStatus(UserStatus.ACTIVE)
                .provider(Provider.LOCAL)
                .build();
    }
}