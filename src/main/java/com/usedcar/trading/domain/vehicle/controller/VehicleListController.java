package com.usedcar.trading.domain.vehicle.controller;

import com.usedcar.trading.domain.favorite.service.FavoriteService;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import com.usedcar.trading.domain.vehicle.entity.FuelType;
import com.usedcar.trading.domain.vehicle.entity.Transmission;
import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.domain.vehicle.entity.VehicleStatus;
import com.usedcar.trading.domain.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class VehicleListController {

    private final VehicleRepository vehicleRepository;
    private final FavoriteService favoriteService;
    private final UserRepository userRepository;

    // 매물 목록 조회 (그리드/리스트)
    @GetMapping
    public String vehicleList(Model model,
                              @RequestParam(required = false) String view,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(required = false) BigDecimal minPrice,
                              @RequestParam(required = false) BigDecimal maxPrice,
                              @RequestParam(required = false) Integer minYear,
                              @RequestParam(required = false) Integer maxYear,
                              @RequestParam(required = false) Integer maxMileage,
                              @RequestParam(required = false) List<FuelType> fuelTypes,
                              @RequestParam(required = false) List<Transmission> transmissions,
                              @AuthenticationPrincipal Object principal,
                              @PageableDefault(size = 9, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        List<Vehicle> filteredList;
        if (keyword != null || minPrice != null || maxPrice != null ||
                minYear != null || maxYear != null || maxMileage != null ||
                (fuelTypes != null && !fuelTypes.isEmpty()) ||
                (transmissions != null && !transmissions.isEmpty())) {

            filteredList = vehicleRepository.searchVehicles(
                    keyword, minPrice, maxPrice, minYear, maxYear, maxMileage, fuelTypes, transmissions
            );
        } else {
            filteredList = vehicleRepository.findByVehicleStatus(VehicleStatus.SALE);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredList.size());
        List<Vehicle> pagedList = (start > filteredList.size()) ? List.of() : filteredList.subList(start, end);

        Page<Vehicle> vehiclePage = new PageImpl<>(pagedList, pageable, filteredList.size());

        model.addAttribute("vehicles", vehiclePage);

        model.addAttribute("keyword", keyword);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("minYear", minYear);
        model.addAttribute("maxYear", maxYear);
        model.addAttribute("maxMileage", maxMileage);
        model.addAttribute("fuelTypes", fuelTypes);
        model.addAttribute("transmissions", transmissions);

        int totalPages = vehiclePage.getTotalPages();
        int nowPage = vehiclePage.getNumber() + 1;
        int startPage = Math.max(nowPage - 2, 1);
        int endPage = Math.min(nowPage + 2, totalPages);
        if(endPage == 0) endPage = 1;

        model.addAttribute("nowPage", nowPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("viewType", view);

        List<Long> userFavoriteIds = new ArrayList<>();
        if (principal != null) {
            User user = findUser(principal);
            if (user != null) {
                userFavoriteIds = favoriteService.getMyFavorites(user.getUserId())
                        .stream()
                        .map(f -> f.getVehicle().getVehicleId())
                        .collect(Collectors.toList());
            }
        }
        model.addAttribute("userFavoriteIds", userFavoriteIds);

        if ("list".equals(view)) {
            return "vehicle-list";
        }
        return "vehicle-grid";
    }

    private User findUser(Object principal) {
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }
}