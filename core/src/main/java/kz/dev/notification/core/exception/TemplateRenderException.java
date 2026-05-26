package kz.dev.notification.core.exception;

import kz.dev.notification.core.enums.NotificationChannel;

public class TemplateRenderException extends RuntimeException {

    public TemplateRenderException(String eventType, NotificationChannel channel, Throwable cause) {
        super("Failed to render template for eventType=%s, channel=%s".formatted(eventType, channel), cause);
    }
}
