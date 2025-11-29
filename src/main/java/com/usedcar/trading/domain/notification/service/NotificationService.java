package com.usedcar.trading.domain.notification.service;

import com.usedcar.trading.domain.notification.entity.Notification;
import com.usedcar.trading.domain.notification.entity.NotificationType;
import com.usedcar.trading.domain.notification.repository.NotificationRepository;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public Notification createNotification(User user, NotificationType type, String message, String link) {
        Notification notification = Notification.builder()
                .user(user)
                .notificationType(type)
                .title(type.getTitle())
                .message(message)
                .link(link)
                .build();

        log.info("알림 생성: userId={}, type={}, message={}", user.getUserId(), type, message);
        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification createNotification(User user, NotificationType type, String link) {
        return createNotification(user, type, type.getDefaultMessage(), link);
    }

    public Page<Notification> getNotifications(User user, Pageable pageable) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
    }

    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Transactional
    public void markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        if (!notification.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalStateException("본인의 알림만 읽음 처리할 수 있습니다.");
        }

        notification.markAsRead();
    }

    @Transactional
    public int markAllAsRead(User user) {
        return notificationRepository.markAllAsRead(user);
    }
}
