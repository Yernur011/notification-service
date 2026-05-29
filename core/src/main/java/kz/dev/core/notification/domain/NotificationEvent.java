package kz.dev.core.notification.domain;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class NotificationEvent {

    String eventType;
    String idempotencyKey;

    @Builder.Default
    String locale = "ru";

    Recipient recipient;

    @Builder.Default
    Map<String, String> templateData = Map.of();
}
