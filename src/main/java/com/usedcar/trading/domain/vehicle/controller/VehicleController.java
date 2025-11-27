package com.usedcar.trading.domain.vehicle.controller;

import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.domain.vehicle.repository.VehicleRepository;
import com.usedcar.trading.domain.vehicle.service.VehicleService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    // 임시
    private final VehicleRepository vehicleRepository;
    private final VehicleService vehicleService;

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
}