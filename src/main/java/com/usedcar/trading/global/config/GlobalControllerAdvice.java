package com.usedcar.trading.global.config;

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

    @ModelAttribute("user")
    public User addUserToModel(@AuthenticationPrincipal Object principal) {
        if (principal instanceof PrincipalDetails) {
            return ((PrincipalDetails) principal).getUser();
        }
        return null;
    }
}