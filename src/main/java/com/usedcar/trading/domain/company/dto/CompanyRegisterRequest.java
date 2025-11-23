package com.usedcar.trading.domain.company.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyRegisterRequest {
    private String businessName;
    private String businessNumber;
    private String address;
}