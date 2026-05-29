package kz.dev.usecase.notification;

import kz.dev.api.notification.GetNotificationLog;
import kz.dev.api.notification.dto.NotificationLogPage;
import kz.dev.api.notification.dto.NotificationLogQuery;
import kz.dev.core.notification.domain.NotificationLog;
import kz.dev.spi.notification.NotificationLogFilter;
import kz.dev.spi.notification.NotificationLogRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class GetNotificationLogUseCase implements GetNotificationLog {

    private final NotificationLogRepository logRepository;

    @Override
    public NotificationLogPage execute(NotificationLogQuery query) {
        NotificationLogFilter filter = NotificationLogFilter.builder()
                .idempotencyKey(query.getIdempotencyKey())
                .eventType(query.getEventType())
                .recipientRef(query.getRecipientRef())
                .from(query.getFrom())
                .to(query.getTo())
                .lastId(query.getLastId())
                .size(query.getSize() + 1)
                .build();

        List<NotificationLog> rows = logRepository.findAll(filter);

        boolean hasNext = rows.size() > query.getSize();
        List<NotificationLog> entries = hasNext ? rows.subList(0, query.getSize()) : rows;
        String nextLastId = hasNext ? entries.get(entries.size() - 1).getId() : null;

        return NotificationLogPage.builder()
                .entries(entries)
                .nextLastId(nextLastId)
                .hasNext(hasNext)
                .build();
    }
}
