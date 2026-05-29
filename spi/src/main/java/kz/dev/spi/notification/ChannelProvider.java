package kz.dev.spi.notification;

import kz.dev.core.notification.domain.RenderedNotification;
import kz.dev.core.notification.enums.NotificationChannel;

public interface ChannelProvider {

    NotificationChannel channel();

    void send(RenderedNotification notification);
}
