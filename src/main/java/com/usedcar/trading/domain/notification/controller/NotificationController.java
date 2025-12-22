package com.usedcar.trading.domain.notification.controller;

import com.usedcar.trading.domain.notification.entity.Notification;
import com.usedcar.trading.domain.notification.service.NotificationService;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.global.auth.security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public String notificationList(Model model,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   @AuthenticationPrincipal PrincipalDetails principal) {

        if (principal == null) return "redirect:/login";

        User user = principal.getUser();

        Page<Notification> notifications = notificationService.getNotifications(user, PageRequest.of(page, size));
        long unreadCount = notificationService.getUnreadCount(user);

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);

        int totalPages = notifications.getTotalPages();
        int nowPage = notifications.getNumber() + 1; // 0부터 시작하므로 +1
        int startPage = Math.max(nowPage - 2, 1);
        int endPage = Math.min(nowPage + 2, totalPages);
        if (endPage == 0) endPage = 1;

        model.addAttribute("nowPage", nowPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("totalPages", totalPages);

        return "notification/list";
    }

    @PostMapping("/{id}/read")
    @ResponseBody
    public String markAsRead(@PathVariable Long id, @AuthenticationPrincipal PrincipalDetails principal) {

        if (principal == null) return "error";

        User user = principal.getUser();

        notificationService.markAsRead(id, user);
        return "success";
    }

    @PostMapping("/read-all")
    @ResponseBody
    public String markAllAsRead(@AuthenticationPrincipal PrincipalDetails principal) {

        if (principal == null) return "error";

        User user = principal.getUser();

        notificationService.markAllAsRead(user);
        return "success";
    }

    @GetMapping("/unread-count")
    @ResponseBody
    public long getUnreadCount(@AuthenticationPrincipal PrincipalDetails principal) {

        if (principal == null) return 0;

        User user = principal.getUser();

        return notificationService.getUnreadCount(user);
    }
}
