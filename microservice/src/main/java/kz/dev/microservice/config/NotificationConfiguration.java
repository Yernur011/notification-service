package kz.dev.microservice.config;

import kz.dev.api.notification.GetNotificationLog;
import kz.dev.api.notification.GetNotificationStatus;
import kz.dev.api.notification.SendNotification;
import kz.dev.spi.notification.ChannelProvider;
import kz.dev.spi.notification.DeduplicationPort;
import kz.dev.spi.notification.NotificationLogRepository;
import kz.dev.spi.notification.RoutingRepository;
import kz.dev.spi.notification.TemplateRepository;
import kz.dev.usecase.notification.GetNotificationLogUseCase;
import kz.dev.usecase.notification.GetNotificationStatusUseCase;
import kz.dev.usecase.notification.SendNotificationUseCase;
import kz.dev.usecase.notification.service.TemplateEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class NotificationConfiguration {

    @Bean
    public TemplateEngine templateEngine() {
        return new TemplateEngine();
    }

    @Bean
    public SendNotification sendNotification(
            DeduplicationPort deduplicationPort,
            RoutingRepository routingRepository,
            TemplateRepository templateRepository,
            TemplateEngine templateEngine,
            List<ChannelProvider> channelProviders,
            NotificationLogRepository logRepository) {
        return new SendNotificationUseCase(
                deduplicationPort, routingRepository, templateRepository,
                templateEngine, channelProviders, logRepository);
    }

    @Bean
    public GetNotificationLog getNotificationLog(NotificationLogRepository logRepository) {
        return new GetNotificationLogUseCase(logRepository);
    }

    @Bean
    public GetNotificationStatus getNotificationStatus(NotificationLogRepository logRepository) {
        return new GetNotificationStatusUseCase(logRepository);
    }
}
