package com.usedcar.trading.global.config;

import com.usedcar.trading.domain.notification.service.NotificationService;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import com.usedcar.trading.global.auth.security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @ModelAttribute("user")
    public User addUserToModel(@AuthenticationPrincipal Object principal) {
        if (principal instanceof PrincipalDetails) {
            return ((PrincipalDetails) principal).getUser();
        }
        return null;
    }

    @ModelAttribute("unreadNotificationCount")
    public long addUnreadCountToModel(@AuthenticationPrincipal Object principal) {
        if (principal instanceof PrincipalDetails) {
            User user = ((PrincipalDetails) principal).getUser();
            return notificationService.getUnreadCount(user);
        }
        return 0L;
    }
}