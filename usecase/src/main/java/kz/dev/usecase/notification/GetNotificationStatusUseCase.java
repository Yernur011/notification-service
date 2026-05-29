package kz.dev.usecase.notification;

import kz.dev.api.notification.GetNotificationStatus;
import kz.dev.core.notification.domain.NotificationLog;
import kz.dev.spi.notification.NotificationLogRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class GetNotificationStatusUseCase implements GetNotificationStatus {

    private final NotificationLogRepository logRepository;

    @Override
    public List<NotificationLog> execute(String idempotencyKey) {
        return logRepository.findByIdempotencyKey(idempotencyKey);
    }
}
