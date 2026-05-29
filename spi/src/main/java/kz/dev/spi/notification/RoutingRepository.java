package kz.dev.spi.notification;

import kz.dev.core.notification.enums.NotificationChannel;

import java.util.List;

public interface RoutingRepository {

    List<NotificationChannel> findChannels(String eventType);
}
