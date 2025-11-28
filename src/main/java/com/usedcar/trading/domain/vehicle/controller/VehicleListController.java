package com.usedcar.trading.domain.vehicle.controller;

import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.domain.vehicle.entity.VehicleStatus;
import com.usedcar.trading.domain.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class VehicleListController {

    private final VehicleRepository vehicleRepository;

    // 매물 목록 조회 (그리드/리스트)
    @GetMapping
    public String vehicleList(Model model,
                              @RequestParam(required = false) String view,
                              @RequestParam(required = false) String keyword,
                              @PageableDefault(size = 9, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        List<Vehicle> allVehicles = vehicleRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        List<Vehicle> filteredList = allVehicles.stream()
                .filter(v -> v.getVehicleStatus() == VehicleStatus.SALE)
                .filter(v -> keyword == null || v.getModel().contains(keyword) || v.getBrand().contains(keyword))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredList.size());
        List<Vehicle> pagedList = (start > filteredList.size()) ? List.of() : filteredList.subList(start, end);

        Page<Vehicle> vehiclePage = new PageImpl<>(pagedList, pageable, filteredList.size());

        model.addAttribute("vehicles", vehiclePage);
        model.addAttribute("keyword", keyword);

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

        if ("list".equals(view)) {
            return "vehicle-list";
        }
        return "vehicle-grid";
    }
}