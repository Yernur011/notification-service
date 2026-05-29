package kz.dev.usecase.notification;

import kz.dev.api.notification.SendNotification;
import kz.dev.api.notification.dto.NotificationResult;
import kz.dev.core.notification.domain.NotificationEvent;
import kz.dev.core.notification.domain.NotificationLog;
import kz.dev.core.notification.domain.NotificationTemplate;
import kz.dev.core.notification.domain.RenderedNotification;
import kz.dev.core.notification.enums.NotificationChannel;
import kz.dev.spi.notification.ChannelProvider;
import kz.dev.spi.notification.DeduplicationPort;
import kz.dev.spi.notification.NotificationLogRepository;
import kz.dev.spi.notification.RoutingRepository;
import kz.dev.spi.notification.TemplateRepository;
import kz.dev.usecase.notification.exception.TemplateNotFoundException;
import kz.dev.usecase.notification.service.TemplateEngine;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class SendNotificationUseCase implements SendNotification {

    private final DeduplicationPort deduplicationPort;
    private final RoutingRepository routingRepository;
    private final TemplateRepository templateRepository;
    private final TemplateEngine templateEngine;
    private final List<ChannelProvider> channelProviders;
    private final NotificationLogRepository logRepository;

    @Override
    public NotificationResult execute(NotificationEvent event) {
        if (deduplicationPort.isDuplicate(event.getIdempotencyKey())) {
            logRepository.save(NotificationLog.skipped(event));
            return NotificationResult.skipped();
        }

        deduplicationPort.markProcessed(event.getIdempotencyKey());

        List<NotificationChannel> channels = routingRepository.findChannels(event.getEventType());

        for (NotificationChannel channel : channels) {
            NotificationLog log = NotificationLog.pending(event, channel);
            try {
                NotificationTemplate template = templateRepository
                        .find(event.getEventType(), channel, event.getLocale())
                        .orElseThrow(() -> new TemplateNotFoundException(event.getEventType(), channel));

                RenderedNotification rendered = templateEngine.render(template, event.getTemplateData());
                rendered.setRecipient(event.getRecipient());

                providerFor(channel).send(rendered);
                log.markSent();
            } catch (Exception e) {
                log.markFailed(e.getMessage());
            } finally {
                logRepository.save(log);
            }
        }

        return NotificationResult.processed();
    }

    private ChannelProvider providerFor(NotificationChannel channel) {
        return channelProviders.stream()
                .filter(p -> p.channel() == channel)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No provider registered for channel: " + channel));
    }
}
