package kz.dev.notification.core.domain;

import kz.dev.notification.core.enums.NotificationChannel;
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
