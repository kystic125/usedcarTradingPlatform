package com.usedcar.trading.domain.notification.controller;

import com.usedcar.trading.domain.notification.entity.Notification;
import com.usedcar.trading.domain.notification.service.NotificationService;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    public String notificationList(HttpSession session, Model model,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Page<Notification> notifications = notificationService.getNotifications(user, PageRequest.of(page, size));
        long unreadCount = notificationService.getUnreadCount(user);

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);

        return "notification/list";
    }

    @PostMapping("/{id}/read")
    @ResponseBody
    public String markAsRead(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "error";
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        notificationService.markAsRead(id, user);
        return "success";
    }

    @PostMapping("/read-all")
    @ResponseBody
    public String markAllAsRead(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "error";
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        notificationService.markAllAsRead(user);
        return "success";
    }

    @GetMapping("/unread-count")
    @ResponseBody
    public long getUnreadCount(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return 0;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return 0;
        }

        return notificationService.getUnreadCount(user);
    }
}
