package com.usedcar.trading.domain.notification.controller;

import com.usedcar.trading.domain.notification.entity.Notification;
import com.usedcar.trading.domain.notification.service.NotificationService;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public String notificationList(Model model,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @AuthenticationPrincipal Object principal) {
        User user = findUser(principal);
        if (user == null) return "redirect:/login";

        Page<Notification> notifications = notificationService.getNotifications(user, PageRequest.of(page, size));
        long unreadCount = notificationService.getUnreadCount(user);

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);

        return "notification/list";
    }

    @PostMapping("/{id}/read")
    @ResponseBody
    public String markAsRead(@PathVariable Long id, @AuthenticationPrincipal Object principal) {
        User user = findUser(principal);
        if (user == null) return "error";

        notificationService.markAsRead(id, user);
        return "success";
    }

    @PostMapping("/read-all")
    @ResponseBody
    public String markAllAsRead(@AuthenticationPrincipal Object principal) {
        User user = findUser(principal);
        if (user == null) return "error";

        notificationService.markAllAsRead(user);
        return "success";
    }

    @GetMapping("/unread-count")
    @ResponseBody
    public long getUnreadCount(@AuthenticationPrincipal Object principal) {
        User user = findUser(principal);
        if (user == null) return 0;

        return notificationService.getUnreadCount(user);
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
}
