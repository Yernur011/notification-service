package kz.dev.core.notification.exception;

import kz.dev.core.notification.enums.NotificationChannel;

public class TemplateRenderException extends RuntimeException {

    public TemplateRenderException(String eventType, NotificationChannel channel, Throwable cause) {
        super("Failed to render template for eventType=%s, channel=%s".formatted(eventType, channel), cause);
    }
}
