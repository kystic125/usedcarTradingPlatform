package com.usedcar.trading.domain.favorite.controller;

import com.usedcar.trading.domain.favorite.entity.Favorite;
import com.usedcar.trading.domain.favorite.service.FavoriteService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
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

    /**
     * 내 찜 목록 [WISH-003]
     */
    @GetMapping
    public String myFavorites(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        List<Favorite> favorites = favoriteService.getMyFavorites(userId);
        int favoriteCount = favoriteService.getMyFavoriteCount(userId);

        model.addAttribute("favorites", favorites);
        model.addAttribute("favoriteCount", favoriteCount);

        return "favorite/list";
    }

    /**
     * 찜 추가 [WISH-001]
     */
    @PostMapping("/add/{vehicleId}")
    public String addFavorite(@PathVariable Long vehicleId,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            favoriteService.addFavorite(userId, vehicleId);
            redirectAttributes.addFlashAttribute("message", "찜 목록에 추가되었습니다.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/vehicles/" + vehicleId;
    }

    /**
     * 찜 삭제 [WISH-002]
     */
    @PostMapping("/remove/{vehicleId}")
    public String removeFavorite(@PathVariable Long vehicleId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes,
                                 @RequestParam(required = false) String returnUrl) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            favoriteService.removeFavorite(userId, vehicleId);
            redirectAttributes.addFlashAttribute("message", "찜 목록에서 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        if (returnUrl != null && returnUrl.equals("list")) {
            return "redirect:/favorites";
        }
        return "redirect:/vehicles/" + vehicleId;
    }

    /**
     * 찜 토글 (AJAX용)
     */
    @PostMapping("/toggle/{vehicleId}")
    @ResponseBody
    public String toggleFavorite(@PathVariable Long vehicleId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "login_required";
        }

        try {
            if (favoriteService.isFavorite(userId, vehicleId)) {
                favoriteService.removeFavorite(userId, vehicleId);
                return "removed";
            } else {
                favoriteService.addFavorite(userId, vehicleId);
                return "added";
            }
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 찜 여부 확인 (AJAX용)
     */
    @GetMapping("/check/{vehicleId}")
    @ResponseBody
    public boolean checkFavorite(@PathVariable Long vehicleId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return false;
        }
        return favoriteService.isFavorite(userId, vehicleId);
    }
}
