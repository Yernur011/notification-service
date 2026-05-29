package kz.dev.core.notification.domain;

import kz.dev.core.notification.enums.NotificationChannel;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NotificationTemplate {

    String id;
    String eventType;
    NotificationChannel channel;
    String locale;
    String subject;
    String body;
}
