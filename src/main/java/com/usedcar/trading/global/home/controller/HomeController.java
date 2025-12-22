package com.usedcar.trading.global.home.controller;

import com.usedcar.trading.domain.favorite.service.FavoriteService;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.domain.vehicle.entity.VehicleStatus;
import com.usedcar.trading.domain.vehicle.repository.VehicleRepository;
import com.usedcar.trading.global.auth.security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final VehicleRepository vehicleRepository;
    private final FavoriteService favoriteService;

    @GetMapping("/")
    public String home(Model model,
                       @CookieValue(value = "recent_cars", required = false) String cookieValue,
                       @AuthenticationPrincipal PrincipalDetails principal) {

        // 최근 등록된 차량 (Top 8)
        List<Vehicle> recentVehicles = vehicleRepository.findByVehicleStatusOrderByCreatedAtDesc(VehicleStatus.SALE)
                .stream().limit(8).collect(Collectors.toList());
        model.addAttribute("recentVehicles", recentVehicles);

        // 조회수가 높은 차량 (Top 8)
        List<Vehicle> popularVehicles = vehicleRepository.findByVehicleStatusOrderByViewCountDesc(VehicleStatus.SALE)
                .stream().limit(8).collect(Collectors.toList());
        model.addAttribute("popularVehicles", popularVehicles);

        // 최근 본 차량 (쿠키)
        List<Vehicle> recentViewedCars = new ArrayList<>();
        if (cookieValue != null && !cookieValue.isEmpty()) {
            String[] ids = cookieValue.split("\\|");
            List<Long> idList = new ArrayList<>();
            for (String s : ids) {
                try { idList.add(Long.parseLong(s)); } catch (NumberFormatException ignored) {}
            }
            if (!idList.isEmpty()) {
                recentViewedCars = vehicleRepository.findAllById(idList);
            }
        }
        model.addAttribute("recentViewedCars", recentViewedCars);

        List<Long> userFavoriteIds = new ArrayList<>();
        if (principal != null) {
            User user = principal.getUser();
            userFavoriteIds = favoriteService.getMyFavorites(user.getUserId())
                    .stream()
                    .map(f -> f.getVehicle().getVehicleId())
                    .collect(Collectors.toList());
        }
        model.addAttribute("userFavoriteIds", userFavoriteIds);

        return "index";
    }
}