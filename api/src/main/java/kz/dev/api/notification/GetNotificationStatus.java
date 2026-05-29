package kz.dev.api.notification;

import kz.dev.core.notification.domain.NotificationLog;

import java.util.List;

@FunctionalInterface
public interface GetNotificationStatus {

    List<NotificationLog> execute(String idempotencyKey);
}
