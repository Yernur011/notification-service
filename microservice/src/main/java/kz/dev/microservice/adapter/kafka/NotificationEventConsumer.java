package kz.dev.microservice.adapter.kafka;

import kz.dev.api.notification.SendNotification;
import kz.dev.core.notification.domain.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final SendNotification sendNotification;

    @KafkaListener(topics = "${notification.kafka.topic}")
    public void consume(NotificationEvent event) {
        log.info("Received event: type={}, key={}", event.getEventType(), event.getIdempotencyKey());
        sendNotification.execute(event);
    }
}
