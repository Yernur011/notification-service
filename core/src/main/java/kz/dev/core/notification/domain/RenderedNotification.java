package kz.dev.core.notification.domain;

import kz.dev.core.notification.enums.NotificationChannel;
import lombok.Data;

@Data
public class RenderedNotification {

    private NotificationChannel channel;
    private Recipient recipient;
    private String subject;
    private String body;
}
