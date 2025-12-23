package com.usedcar.trading.domain.company.service;

import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.company.repository.CompanyRepository;
import com.usedcar.trading.domain.employee.entity.Employee;
import com.usedcar.trading.domain.employee.repository.EmployeeRepository;
import com.usedcar.trading.domain.transaction.entity.TransactionStatus;
import com.usedcar.trading.domain.user.entity.Role;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.domain.vehicle.entity.VehicleStatus;
import com.usedcar.trading.domain.vehicle.repository.VehicleRepository;
import com.usedcar.trading.global.exception.CustomException;
import com.usedcar.trading.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesService {

    private final VehicleRepository vehicleRepository;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;

    public Page<Vehicle> getMySalesVehicles(User user, String filter, Pageable pageable) {
        List<Vehicle> allVehicles = findUserVehicles(user);
        List<Vehicle> filteredVehicles = filterVehicles(allVehicles, filter);
        List<Vehicle> sortedVehicles = sortByPriority(filteredVehicles);

        return toPage(sortedVehicles, pageable);
    }

    private List<Vehicle> findUserVehicles(User user) {
        if (user.getRole() == Role.COMPANY_OWNER) {
            Company company = companyRepository.findByOwner_UserId(user.getUserId())
                    .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

            return vehicleRepository.findByCompany(company);
        } else if (user.getRole() == Role.COMPANY_EMPLOYEE) {
            Employee employee = employeeRepository.findByUserUserId(user.getUserId())
                    .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_COMPANY_NOT_FOUND));

            return vehicleRepository.findByRegisteredBy(employee);
        }
        throw new CustomException(ErrorCode.UNAUTHORIZED_SELLER_ROLE);
    }

    private List<Vehicle> filterVehicles(List<Vehicle> vehicles, String filter) {
        if ("REQUESTED".equals(filter)) {
            return vehicles.stream().filter(this::hasRequestedTransaction).collect(Collectors.toList());
        } else if ("SALE".equals(filter)) {
            return vehicles.stream().filter(v -> v.getVehicleStatus() == VehicleStatus.SALE).collect(Collectors.toList());
        } else if ("SOLD".equals(filter)) {
            return vehicles.stream().filter(v -> v.getVehicleStatus() == VehicleStatus.SOLD).collect(Collectors.toList());
        } else if ("PENDING".equals(filter)) {
            return vehicles.stream().filter(v -> v.getVehicleStatus() == VehicleStatus.PENDING).collect(Collectors.toList());
        } else if ("REJECTED".equals(filter)) {
            return vehicles.stream().filter(v -> v.getVehicleStatus() == VehicleStatus.REJECTED).collect(Collectors.toList());
        } else if ("EXPIRED".equals(filter)) {
            return vehicles.stream().filter(v -> v.getVehicleStatus() == VehicleStatus.EXPIRED).collect(Collectors.toList());
        }

        return vehicles;
    }

    private List<Vehicle> sortByPriority(List<Vehicle> vehicles) {
        return vehicles.stream()
                .sorted(Comparator.comparingInt(this::getPriorityScore))
                .collect(Collectors.toList());
    }

    private int getPriorityScore(Vehicle vehicle) {
        if (hasRequestedTransaction(vehicle)) {
            return 1;
        }
        if (vehicle.getTransactions().stream().anyMatch(t -> t.getTransactionStatus() == TransactionStatus.APPROVED)) {
            return 2;
        }
        if (vehicle.getVehicleStatus() == VehicleStatus.SALE) {
            return 3;
        }
        if (vehicle.getVehicleStatus() == VehicleStatus.SOLD) {
            return 5;
        }
        return 4;
    }

    private boolean hasRequestedTransaction(Vehicle vehicle) {
        return vehicle.getTransactions().stream()
                .anyMatch(t -> t.getTransactionStatus() == TransactionStatus.REQUESTED);
    }

    private Page<Vehicle> toPage(List<Vehicle> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());
        List<Vehicle> pagedList = (start > list.size() ? List.of() : list.subList(start, end));

        return new PageImpl<>(pagedList, pageable, list.size());
    }
}
