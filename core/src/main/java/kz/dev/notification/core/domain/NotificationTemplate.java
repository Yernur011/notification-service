package kz.dev.notification.core.domain;

import kz.dev.notification.core.enums.NotificationChannel;
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
