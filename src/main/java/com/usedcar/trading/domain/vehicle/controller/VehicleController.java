package com.usedcar.trading.domain.vehicle.controller;

import com.usedcar.trading.domain.favorite.service.FavoriteService;
import com.usedcar.trading.domain.review.entity.Review;
import com.usedcar.trading.domain.review.service.ReviewService;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import com.usedcar.trading.domain.vehicle.dto.VehicleRegisterRequest;
import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.domain.vehicle.repository.VehicleRepository;
import com.usedcar.trading.domain.vehicle.service.VehicleService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
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
    private final FavoriteService favoriteService;
    private final ReviewService reviewService;

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
            return userRepository.findByEmail(email).orElse(null);
        } else if (principal instanceof OAuth2User) {
            OAuth2User oauthUser = (OAuth2User) principal;
            String providerId = String.valueOf(oauthUser.getAttributes().get("id"));
            return userRepository.findByProviderId(providerId).orElse(null);
        }
        return null;
    }

    // 매물 상세 조회
    @GetMapping("/{id}")
    public String vehicleDetail(@PathVariable Long id,
                                Model model,
                                @RequestParam(defaultValue = "0") int reviewPage,
                                @CookieValue(value = "recent_cars", required = false) String cookieValue,
                                @AuthenticationPrincipal Object principal,
                                HttpServletResponse response) {

        vehicleService.increaseViewCount(id);

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 매물이 존재하지 않습니다. id=" + id));

        model.addAttribute("car", vehicle);

        if (vehicle.getCompany() != null) {
            Long companyId = vehicle.getCompany().getCompanyId();

            // 페이징: 5개씩, 최신순
            Page<Review> reviewList = reviewService.getCompanyReviews(
                    companyId,
                    PageRequest.of(reviewPage, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
            );

            model.addAttribute("sellerReviews", reviewList);
            model.addAttribute("reviewPage", reviewPage);

            // 평점 평균 및 개수
            model.addAttribute("avgRating", reviewService.getCompanyAverageRating(companyId));
            model.addAttribute("reviewCount", reviewService.getCompanyReviewCount(companyId));
        }

        boolean isFavorite = false;
        if (principal != null) {
            User user = findUser(principal);
            if (user != null) {
                isFavorite = favoriteService.isFavorite(user.getUserId(), id);
            }
        }
        model.addAttribute("isFavorite", isFavorite);

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

        boolean isRegistrant = vehicle.getRegisteredBy().getUser().getUserId().equals(user.getUserId());
        boolean isBoss = (user.getRole() == com.usedcar.trading.domain.user.entity.Role.COMPANY_OWNER) &&
                vehicle.getCompany().getOwner().getUserId().equals(user.getUserId());

        if (!isRegistrant && !isBoss) {
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