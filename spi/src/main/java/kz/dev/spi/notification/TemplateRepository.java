package kz.dev.spi.notification;

import kz.dev.core.notification.domain.NotificationTemplate;
import kz.dev.core.notification.enums.NotificationChannel;

import java.util.Optional;

public interface TemplateRepository {

    Optional<NotificationTemplate> find(String eventType, NotificationChannel channel, String locale);
}
