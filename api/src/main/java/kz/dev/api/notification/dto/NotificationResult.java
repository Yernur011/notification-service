package kz.dev.api.notification.dto;

import lombok.Value;

@Value
public class NotificationResult {

    Status status;

    public static NotificationResult processed() {
        return new NotificationResult(Status.PROCESSED);
    }

    public static NotificationResult skipped() {
        return new NotificationResult(Status.SKIPPED);
    }

    public enum Status {
        PROCESSED, SKIPPED
    }
}
