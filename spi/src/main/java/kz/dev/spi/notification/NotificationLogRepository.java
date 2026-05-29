package kz.dev.spi.notification;

import kz.dev.core.notification.domain.NotificationLog;

import java.util.List;

public interface NotificationLogRepository {

    void save(NotificationLog log);

    List<NotificationLog> findByIdempotencyKey(String idempotencyKey);

    List<NotificationLog> findAll(NotificationLogFilter filter);
}
