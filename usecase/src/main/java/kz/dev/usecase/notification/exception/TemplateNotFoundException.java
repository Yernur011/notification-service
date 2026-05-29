package kz.dev.usecase.notification.exception;

import kz.dev.core.notification.enums.NotificationChannel;

public class TemplateNotFoundException extends RuntimeException {

    public TemplateNotFoundException(String eventType, NotificationChannel channel) {
        super("Template not found for eventType=%s, channel=%s".formatted(eventType, channel));
    }
}
