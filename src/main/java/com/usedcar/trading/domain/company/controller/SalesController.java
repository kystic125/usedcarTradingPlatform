package com.usedcar.trading.domain.company.controller;

import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.company.repository.CompanyRepository;
import com.usedcar.trading.domain.employee.entity.Employee;
import com.usedcar.trading.domain.employee.repository.EmployeeRepository;
import com.usedcar.trading.domain.transaction.entity.Transaction;
import com.usedcar.trading.domain.transaction.entity.TransactionStatus;
import com.usedcar.trading.domain.user.entity.Role;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.domain.vehicle.entity.VehicleStatus;
import com.usedcar.trading.domain.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/company/sales")
@RequiredArgsConstructor
public class SalesController {

    private final VehicleRepository vehicleRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    @GetMapping
    public String salesDashboard(Model model,
                                 @AuthenticationPrincipal Object principal,
                                 @RequestParam(required = false, defaultValue = "ALL") String filter,
                                 @PageableDefault(size = 10) Pageable pageable) {

        User user = findUser(principal);

        List<Vehicle> allVehicles;

        if (user.getRole() == Role.COMPANY_OWNER) {
            Company company = companyRepository.findByOwner_UserId(user.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("등록된 업체가 없습니다."));
            allVehicles = vehicleRepository.findByCompany(company);
        } else if (user.getRole() == Role.COMPANY_EMPLOYEE) {
            Employee employee = employeeRepository.findByUserUserId(user.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("소속된 회사가 없습니다."));
            allVehicles = vehicleRepository.findByRegisteredBy(employee);
        } else {
            throw new IllegalStateException("판매자 권한이 아닙니다.");
        }

        List<Vehicle> filteredList;
        if ("REQUESTED".equals(filter)) {
            filteredList = allVehicles.stream().filter(this::hasRequestedTransaction).collect(Collectors.toList());
        } else if ("SALE".equals(filter)) {
            filteredList = allVehicles.stream().filter(v -> v.getVehicleStatus() == VehicleStatus.SALE).collect(Collectors.toList());
        } else if ("RESERVED".equals(filter)) {
            filteredList = allVehicles.stream().filter(v -> v.getVehicleStatus() == VehicleStatus.RESERVED).collect(Collectors.toList());
        } else if ("SOLD".equals(filter)) {
            filteredList = allVehicles.stream().filter(v -> v.getVehicleStatus() == VehicleStatus.SOLD).collect(Collectors.toList());
        } else if ("PENDING".equals(filter)) {
            filteredList = allVehicles.stream().filter(v -> v.getVehicleStatus() == VehicleStatus.PENDING).collect(Collectors.toList());
        } else if ("REJECTED".equals(filter)) {
            filteredList = allVehicles.stream().filter(v -> v.getVehicleStatus() == VehicleStatus.REJECTED).collect(Collectors.toList());
        } else if ("EXPIRED".equals(filter)) {
            filteredList = allVehicles.stream().filter(v -> v.getVehicleStatus() == VehicleStatus.EXPIRED).collect(Collectors.toList());
        } else {
            filteredList = allVehicles;
        }

        filteredList.sort(Comparator.comparingInt(this::getPriorityScore));

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredList.size());

        List<Vehicle> pagedList;
        if (start > filteredList.size()) {
            pagedList = List.of();
        } else {
            pagedList = filteredList.subList(start, end);
        }

        Page<Vehicle> vehiclePage = new PageImpl<>(pagedList, pageable, filteredList.size());

        model.addAttribute("vehicles", vehiclePage);
        model.addAttribute("currentFilter", filter);

        int totalPages = vehiclePage.getTotalPages();
        int nowPage = vehiclePage.getNumber() + 1;
        int startPage = Math.max(nowPage - 2, 1);
        int endPage = Math.min(nowPage + 2, totalPages);
        if(endPage == 0) endPage = 1;

        model.addAttribute("nowPage", nowPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("totalPages", totalPages);

        return "company/sales-list";
    }

    private int getPriorityScore(Vehicle v) {
        if (hasRequestedTransaction(v)) return 1;
        if (v.getTransactions().stream().anyMatch(t -> t.getTransactionStatus() == TransactionStatus.APPROVED)) return 2;
        if (v.getVehicleStatus() == VehicleStatus.SALE) return 3;
        if (v.getVehicleStatus() == VehicleStatus.SOLD) return 5;
        return 4;
    }

    private boolean hasRequestedTransaction(Vehicle v) {
        return v.getTransactions().stream()
                .anyMatch(t -> t.getTransactionStatus() == TransactionStatus.REQUESTED);
    }

    private User findUser(Object principal) {
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            return userRepository.findByEmail(email).orElseThrow();
        }
        throw new IllegalArgumentException("로그인이 필요합니다.");
    }
}