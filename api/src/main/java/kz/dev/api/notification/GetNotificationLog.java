package kz.dev.api.notification;

import kz.dev.api.notification.dto.NotificationLogPage;
import kz.dev.api.notification.dto.NotificationLogQuery;

@FunctionalInterface
public interface GetNotificationLog {

    NotificationLogPage execute(NotificationLogQuery query);
}
