# Architecture

Notification Service is built on **Hexagonal Architecture** (Ports & Adapters). Business logic in `core` and `usecase` has zero dependency on Spring, Kafka, or any infrastructure concern. External systems connect through ports (interfaces) implemented by adapters.

## Module structure

```
notification-service/
├── api/          # Inbound ports (interfaces) — what the service exposes
├── spi/          # Outbound ports (interfaces) — what the service requires
├── core/         # Domain objects, value objects, enums
├── usecase/      # Business logic — implements api, depends only on spi + core
└── microservice/ # Spring Boot entry point, assembles all modules
```

Future adapter modules (planned):

```
api-adapter-kafka/     # Inbound: Kafka consumer
api-adapter-webmvc/    # Inbound: REST (optional)
spi-adapter-jpa/       # Outbound: PostgreSQL (templates, routing, logs)
spi-adapter-redis/     # Outbound: Redis (deduplication)
spi-adapter-email/     # Outbound: SendGrid / AWS SES
spi-adapter-push/      # Outbound: FCM / APNs
spi-adapter-sms/       # Outbound: Twilio
spi-adapter-chat/      # Outbound: Slack / Telegram
```

## Dependency diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        microservice                         │
│  ┌────────────┐    ┌───────────────────────┐   ┌─────────┐  │
│  │api-adapter │    │       usecase         │   │spi-     │  │
│  │ -webmvc    │───▶│  implements api ports │──▶│adapter  │  │
│  │ -kafka     │    │  uses spi ports       │   │ -kafka  │  │
│  └────────────┘    └───────────────────────┘   │ -jpa    │  │
│        │                     │                 │ -redis  │  │
│        ▼                     ▼                 └─────────┘  │
│  ┌──────────┐       ┌──────────────┐                        │
│  │   api    │       │     spi      │                        │
│  │ (inbound │       │ (outbound    │                        │
│  │  ports)  │       │  ports)      │                        │
│  └──────────┘       └──────────────┘                        │
│        │                   │                                │
│        └──────────┬────────┘                                │
│                   ▼                                         │
│              ┌─────────┐                                    │
│              │  core   │                                    │
│              │ (domain)│                                    │
│              └─────────┘                                    │
└─────────────────────────────────────────────────────────────┘
```

## Notification processing flow

```
Kafka topic: notifications
        │
        ▼
NotificationEventConsumer (@KafkaListener)
        │  deserialize JSON → NotificationEvent
        ▼
NotificationUseCase.handle(request)
        │
        ├──▶ DeduplicationPort.isDuplicate(idempotencyKey)
        │         duplicate → SKIPPED, exit
        │
        ├──▶ RoutingRepository.findChannels(eventType)
        │         → [EMAIL, PUSH]
        │
        └──▶ for each channel:
                  │
                  ├──▶ TemplateRepository.find(eventType, channel, locale)
                  ├──▶ TemplateEngine.render(template, templateData)
                  ├──▶ ChannelProvider.send(rendered)
                  └──▶ NotificationLogRepository.save(log)
```

## Technology stack

| Component      | Technology                          |
|----------------|-------------------------------------|
| Language       | Java 21                             |
| Framework      | Spring Boot 3.x                     |
| Messaging      | Apache Kafka                        |
| Database       | PostgreSQL + Spring Data JPA        |
| Cache / Dedup  | Redis (Spring Data Redis)           |
| Templates      | Freemarker                          |
| Unit tests     | JUnit 5 + Mockito                   |
| Email          | SendGrid / AWS SES                  |
| Push           | FCM + APNs (firebase-admin)         |
| SMS            | Twilio                              |
| Chat           | Slack Webhooks / Telegram Bot API   |
| Build          | Maven (multi-module)                |
| Containers     | Docker + docker-compose             |
