package kz.dev.notification.core.domain;

import kz.dev.notification.core.enums.NotificationChannel;
import lombok.Data;

@Data
public class RenderedNotification {

    private NotificationChannel channel;
    private Recipient recipient;
    private String subject;
    private String body;
}
