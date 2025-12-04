package com.usedcar.trading.domain.vehicle.controller;

import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import com.usedcar.trading.domain.vehicle.dto.VehicleRegisterRequest;
import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.domain.vehicle.repository.VehicleRepository;
import com.usedcar.trading.domain.vehicle.service.VehicleService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleRepository vehicleRepository;
    private final VehicleService vehicleService;
    private final UserRepository userRepository;

    // 매물 등록 페이지
    @GetMapping("/register")
    public String registerPage(Model model) {
        return "vehicle/register";
    }

    // 매물 등록 처리
    @PostMapping("/register")
    public String registerVehicle(
            @ModelAttribute VehicleRegisterRequest request,
            @RequestParam("imageFiles") List<MultipartFile> imageFiles,
            @AuthenticationPrincipal Object principal) {

        User user = findUser(principal);

        try {
            vehicleService.register(user, request, imageFiles);
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/company/sales?error=" + e.getMessage();
        }

        return "redirect:/company/sales";
    }

    private User findUser(Object principal) {
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("회원 정보 없음"));
        }
        throw new IllegalArgumentException("로그인이 필요합니다.");
    }

    // 매물 상세 조회
    @GetMapping("/{id}")
    public String vehicleDetail(@PathVariable Long id,
                                Model model,
                                @CookieValue(value = "recent_cars", required = false) String cookieValue,
                                HttpServletResponse response) {

        vehicleService.increaseViewCount(id);

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 매물이 존재하지 않습니다. id=" + id));

        model.addAttribute("car", vehicle);

        List<Long> recentIds = new ArrayList<>();
        if (cookieValue != null && !cookieValue.isEmpty()) {
            String[] ids = cookieValue.split("\\|");
            for (String s : ids) {
                try { recentIds.add(Long.parseLong(s)); } catch (NumberFormatException ignored) {}
            }
        }

        recentIds.remove(id);
        recentIds.add(0, id);
        if (recentIds.size() > 5) recentIds = recentIds.subList(0, 5);

        String newCookieValue = recentIds.stream().map(String::valueOf).collect(Collectors.joining("|"));
        Cookie cookie = new Cookie("recent_cars", newCookieValue);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24);
        response.addCookie(cookie);

        List<Long> idsToView = new ArrayList<>(recentIds);
        idsToView.remove(id);

        List<Vehicle> recentCars = new ArrayList<>();
        if (!idsToView.isEmpty()) {
            recentCars = vehicleRepository.findAllById(idsToView);
        }
        model.addAttribute("recentCars", recentCars);

        return "vehicle-detail";
    }

    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable Long id, Model model, @AuthenticationPrincipal Object principal) {
        User user = findUser(principal);

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("매물 없음"));

        if (!vehicle.getRegisteredBy().getUser().getUserId().equals(user.getUserId())) {
            return "redirect:/company/sales?error=unauthorized";
        }

        model.addAttribute("car", vehicle);
        return "vehicle/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateVehicle(@PathVariable Long id,
                                @ModelAttribute VehicleRegisterRequest request,
                                @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles, // [추가] 파일 받기
                                @AuthenticationPrincipal Object principal) {
        User user = findUser(principal);

        try {
            vehicleService.update(id, user, request, imageFiles);
        } catch (Exception e) {
            return "redirect:/vehicles/" + id + "/edit?error=" + e.getMessage();
        }

        return "redirect:/vehicles/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteVehicle(@PathVariable Long id, @AuthenticationPrincipal Object principal) {
        User user = findUser(principal);

        try {
            vehicleService.delete(id, user);
        } catch (Exception e) {
            return "redirect:/company/sales?error=" + e.getMessage();
        }

        return "redirect:/company/sales";
    }
}