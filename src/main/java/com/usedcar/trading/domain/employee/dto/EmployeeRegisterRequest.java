package com.usedcar.trading.domain.employee.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeRegisterRequest {
    private String name;
    private String email;
    private String password;
    private String phone;
}