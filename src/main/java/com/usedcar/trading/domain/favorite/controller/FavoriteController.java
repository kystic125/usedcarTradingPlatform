package com.usedcar.trading.domain.favorite.controller;

import com.usedcar.trading.domain.favorite.dto.FavoriteResponse;
import com.usedcar.trading.domain.favorite.service.FavoriteService;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserRepository userRepository;

    // 내 찜 목록
    @GetMapping
    public String myFavorites(Model model, @AuthenticationPrincipal Object principal) {
        User user = findUser(principal);

        if (user == null) {
            return "redirect:/login";
        }

        List<FavoriteResponse> favorites = favoriteService.getMyFavorites(user.getUserId()).stream()
                .map(FavoriteResponse::toResponse)
                .toList();
        model.addAttribute("favorites", favorites);

        int favoriteCount = favoriteService.getMyFavoriteCount(user.getUserId());
        model.addAttribute("favoriteCount", favoriteCount);
        model.addAttribute("user", user);

        return "favorite/list";
    }
    
    // 찜 추가
    @PostMapping("/add/{vehicleId}")
    public String addFavorite(@PathVariable Long vehicleId,
                              @AuthenticationPrincipal Object principal,
                              RedirectAttributes redirectAttributes) {
        User user = findUser(principal);
        if (user == null) return "redirect:/login";

        favoriteService.addFavorite(user.getUserId(), vehicleId);
        redirectAttributes.addFlashAttribute("message", "찜 목록에 추가되었습니다.");

        return "redirect:/vehicles/" + vehicleId;
    }
    
    // 찜 삭제
    @PostMapping("/remove/{vehicleId}")
    public String removeFavorite(@PathVariable Long vehicleId,
                                 @AuthenticationPrincipal Object principal,
                                 RedirectAttributes redirectAttributes,
                                 @RequestParam(required = false) String returnUrl) {
        User user = findUser(principal);
        if (user == null) return "redirect:/login";

        favoriteService.removeFavorite(user.getUserId(), vehicleId);
        redirectAttributes.addFlashAttribute("message", "찜 목록에서 삭제되었습니다.");

        if ("list".equals(returnUrl)) {
            return "redirect:/favorites";
        }
        return "redirect:/vehicles/" + vehicleId;
    }

    // 찜 토글
    @PostMapping("/toggle/{vehicleId}")
    @ResponseBody
    public String toggleFavorite(@PathVariable Long vehicleId, @AuthenticationPrincipal Object principal) {
        User user = findUser(principal);
        if (user == null) {
            return "login required";
        }

        if (favoriteService.isFavorite(user.getUserId(), vehicleId)) {
            favoriteService.removeFavorite(user.getUserId(), vehicleId);
            return "removed";
        } else {
            favoriteService.addFavorite(user.getUserId(), vehicleId);
            return "added";
        }
    }

    // 찜 여부 확인
    @GetMapping("/check/{vehicleId}")
    @ResponseBody
    public boolean checkFavorite(@PathVariable Long vehicleId, @AuthenticationPrincipal Object principal) {
        User user = findUser(principal);

        if (user == null) {
            return false;
        }

        return favoriteService.isFavorite(user.getUserId(), vehicleId);
    }

    private User findUser(Object principal) {
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            return userRepository.findByEmail(email).orElse(null);
        } else if (principal instanceof OAuth2User) {
            // 소셜 로그인 처리 (필요 시 구체화)
            OAuth2User oauthUser = (OAuth2User) principal;
            String providerId = String.valueOf(oauthUser.getAttributes().get("id")); // 카카오 ID 예시
            return userRepository.findByProviderId(providerId).orElse(null);
        }
        return null;
    }
}
