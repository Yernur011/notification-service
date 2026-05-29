package kz.dev.api.notification;

import kz.dev.api.notification.dto.NotificationResult;
import kz.dev.core.notification.domain.NotificationEvent;

@FunctionalInterface
public interface SendNotification {

    NotificationResult execute(NotificationEvent event);
}
