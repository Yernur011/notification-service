package kz.dev.spi.notification;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class NotificationLogFilter {

    String idempotencyKey;
    String eventType;
    String recipientRef;
    Instant from;
    Instant to;
    String lastId;

    @Builder.Default
    int size = 20;
}
