package kz.dev.microservice.adapter.web;

import kz.dev.api.notification.GetNotificationLog;
import kz.dev.api.notification.GetNotificationStatus;
import kz.dev.api.notification.SendNotification;
import kz.dev.api.notification.dto.NotificationLogPage;
import kz.dev.api.notification.dto.NotificationLogQuery;
import kz.dev.api.notification.dto.NotificationResult;
import kz.dev.core.notification.domain.NotificationEvent;
import kz.dev.core.notification.domain.NotificationLog;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SendNotification sendNotification;
    private final GetNotificationLog getNotificationLog;
    private final GetNotificationStatus getNotificationStatus;

    @PostMapping
    public NotificationResult send(@RequestBody NotificationEvent event) {
        return sendNotification.execute(event);
    }

    @GetMapping("/log")
    public NotificationLogPage log(
            @RequestParam(required = false) String idempotencyKey,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String recipientRef,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String lastId,
            @RequestParam(defaultValue = "20") int size) {
        NotificationLogQuery query = NotificationLogQuery.builder()
                .idempotencyKey(idempotencyKey)
                .eventType(eventType)
                .recipientRef(recipientRef)
                .from(from)
                .to(to)
                .lastId(lastId)
                .size(size)
                .build();
        return getNotificationLog.execute(query);
    }

    @GetMapping("/status/{idempotencyKey}")
    public List<NotificationLog> status(@PathVariable String idempotencyKey) {
        return getNotificationStatus.execute(idempotencyKey);
    }
}
