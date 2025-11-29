package com.usedcar.trading.domain.admin.controller;

import com.usedcar.trading.domain.admin.service.AdminService;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;

    /**
     * 관리자 대시보드 [ADM-005]
     */
    @GetMapping
    public String adminDashboard(Model model) {
        model.addAttribute("stats", adminService.getDashboardStats());
        return "admin/dashboard";
    }

    /**
     * 매물 통계 API [ADM-006]
     */
    @GetMapping("/stats/vehicles")
    @ResponseBody
    public java.util.Map<String, Long> getVehicleStats() {
        return adminService.getVehicleStats();
    }

    /**
     * 거래 통계 API [ADM-007]
     */
    @GetMapping("/stats/transactions")
    @ResponseBody
    public java.util.Map<String, Long> getTransactionStats() {
        return adminService.getTransactionStats();
    }

    /**
     * 신고 통계 API [ADM-008]
     */
    @GetMapping("/stats/reports")
    @ResponseBody
    public java.util.Map<String, Long> getReportStats() {
        return adminService.getReportStats();
    }

    @GetMapping("/vehicles")
    public String pendingVehicleList(Model model) {
        List<Vehicle> pendingCars = adminService.getPendingVehicles();
        model.addAttribute("cars", pendingCars);
        return "admin/vehicle-list";
    }

    @PostMapping("/vehicles/{id}/approve")
    public String approveVehicle(@PathVariable Long id, @AuthenticationPrincipal Object principal) {
        User admin = findUser(principal);

        adminService.approveVehicle(id, admin);
        return "redirect:/admin/vehicles";
    }

    @PostMapping("/vehicles/{id}/reject")
    public String rejectVehicle(@PathVariable Long id,
                                @RequestParam String reason,
                                @AuthenticationPrincipal Object principal) {
        User admin = findUser(principal);
        adminService.rejectVehicle(id, reason, admin);
        return "redirect:/admin/vehicles";
    }

    private User findUser(Object principal) {
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            String email = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            return userRepository.findByEmail(email).orElseThrow();
        }

        throw new IllegalArgumentException("관리자 정보를 찾을 수 없습니다.");
    }
}