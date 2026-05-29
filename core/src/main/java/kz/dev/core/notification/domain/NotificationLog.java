package kz.dev.core.notification.domain;

import kz.dev.core.notification.enums.NotificationChannel;
import kz.dev.core.notification.enums.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog {

    private String id;
    private String idempotencyKey;
    private String eventType;
    private NotificationChannel channel;
    private String recipientRef;
    private NotificationStatus status;
    private String errorMessage;

    @Builder.Default
    private Instant createdAt = Instant.now();

    public static NotificationLog pending(NotificationEvent event, NotificationChannel channel) {
        return NotificationLog.builder()
                .idempotencyKey(event.getIdempotencyKey())
                .eventType(event.getEventType())
                .channel(channel)
                .recipientRef(event.getRecipient().ref())
                .status(NotificationStatus.PENDING)
                .build();
    }

    public static NotificationLog skipped(NotificationEvent event) {
        return NotificationLog.builder()
                .idempotencyKey(event.getIdempotencyKey())
                .eventType(event.getEventType())
                .status(NotificationStatus.SKIPPED)
                .build();
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
    }

    public void markFailed(String message) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = message;
    }
}
