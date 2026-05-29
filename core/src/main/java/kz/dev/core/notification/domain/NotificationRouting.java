package kz.dev.core.notification.domain;

import kz.dev.core.notification.enums.NotificationChannel;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NotificationRouting {

    String id;
    String eventType;
    NotificationChannel channel;
    boolean enabled;
}
