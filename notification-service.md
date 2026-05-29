# Notification Service

Микросервис асинхронной доставки уведомлений через Email, Push, SMS и Chat-каналы.
Построен на гексагональной архитектуре, Spring Boot, Kafka.

---

## Содержание

1. [Архитектурный обзор](#архитектурный-обзор)
2. [Структура модулей](#структура-модулей)
3. [Модули подробно](#модули-подробно)
4. [Поток обработки уведомления](#поток-обработки-уведомления)
5. [Контракты](#контракты)
6. [Схема базы данных](#схема-базы-данных)
7. [Конфигурация](#конфигурация)
8. [Технологический стек](#технологический-стек)

---

## Архитектурный обзор

Сервис построен по **Hexagonal Architecture** (Ports & Adapters).
Бизнес-логика в `core` и `usecase` не зависит ни от фреймворка, ни от инфраструктуры.
Все внешние зависимости подключаются через порты (интерфейсы) и адаптеры (реализации).

```
┌─────────────────────────────────────────────────────────────┐
│                        microservice                         │
│  ┌────────────┐    ┌───────────────────────┐   ┌─────────┐  │
│  │api-adapter │    │       usecase         │   │spi-     │  │
│  │ -webmvc    │───▶│  реализует API-порты  │──▶│adapter  │  │
│  │ -kafka     │    │  использует SPI-порты │   │ -kafka  │  │
│  └────────────┘    └───────────────────────┘   │ -jpa    │  │
│        │                     │                 │ -redis  │  │
│        ▼                     ▼                 └─────────┘  │
│  ┌──────────┐       ┌──────────────┐                        │
│  │   api    │       │     spi      │                        │
│  │ (порты   │       │ (порты вовне)│                        │
│  │  входа)  │       └──────────────┘                        │
│  └──────────┘                │                              │
│        │             ┌───────▼──────┐                       │
│        └────────────▶│     core     │                       │
│                       │  (домен)    │                       │
│                       └─────────────┘                       │
└─────────────────────────────────────────────────────────────┘
```

---

## Структура модулей

```
notification-service/
├── api/                          # Входные порты (интерфейсы)
├── spi/                          # Выходные порты (интерфейсы)
├── core/                         # Доменные объекты, value objects, enums
├── usecase/                      # Бизнес-логика, реализует api, использует spi
├── api-adapter-kafka/            # Входящий адаптер: Kafka consumer
├── api-adapter-webmvc/           # Входящий адаптер: REST API (опционально)
├── spi-adapter-kafka/            # Исходящий адаптер: публикация в Kafka
├── spi-adapter-jpa/              # Исходящий адаптер: PostgreSQL
├── spi-adapter-redis/            # Исходящий адаптер: Redis (dedup, cache)
├── spi-adapter-email/            # Исходящий адаптер: SendGrid / SES
├── spi-adapter-push/             # Исходящий адаптер: FCM / APNs
├── spi-adapter-sms/              # Исходящий адаптер: Twilio
├── spi-adapter-chat/             # Исходящий адаптер: Slack / Telegram
└── microservice/                 # Spring Boot entry point, сборка всех модулей
```

---

## Модули подробно

### `api` — входные порты

Описывает **что умеет делать сервис** с точки зрения внешнего мира.
Только интерфейсы и DTO. Нет зависимостей от Spring, Kafka или JPA.

```
api/src/main/java/com/example/notification/api/
├── NotificationUseCase.java          # главный входной порт
├── dto/
│   ├── NotificationRequest.java      # что приходит снаружи
│   └── NotificationResult.java       # что возвращаем
└── exception/
    └── NotificationException.java
```

### `spi` — выходные порты

Описывает **что нужно сервису от инфраструктуры**.
Только интерфейсы. Реализации — в `spi-adapter-*`.

```
spi/src/main/java/com/example/notification/spi/
├── ChannelProvider.java              # отправка через конкретный канал
├── TemplateRepository.java           # получение шаблонов
├── RoutingRepository.java            # routing rules: eventType → каналы
├── NotificationLogRepository.java    # сохранение истории
└── DeduplicationPort.java            # проверка idempotency key
```

### `core` — доменная модель

Чистые Java-объекты. Никаких аннотаций фреймворков.

```
core/src/main/java/com/example/notification/core/
├── domain/
│   ├── NotificationEvent.java        # входящее событие из Kafka
│   ├── RenderedNotification.java      # готовое сообщение для отправки
│   ├── NotificationLog.java           # запись об отправке
│   ├── NotificationTemplate.java      # шаблон
│   └── NotificationRouting.java       # правило маршрутизации
├── enums/
│   ├── NotificationChannel.java       # EMAIL, PUSH, SMS, CHAT
│   └── NotificationStatus.java        # SENT, FAILED, SKIPPED, PENDING
└── exception/
    └── TemplateRenderException.java
```

### `usecase` — бизнес-логика

Реализует интерфейсы из `api`. Использует только интерфейсы из `spi` и классы из `core`.
Никакой зависимости от Spring (кроме аннотаций `@Component` / `@Transactional` при необходимости).

```
usecase/src/main/java/com/example/notification/usecase/
└── SendNotificationUseCase.java      # реализует NotificationUseCase
```

**Логика внутри `SendNotificationUseCase`:**

1. Проверить дедупликацию через `DeduplicationPort`
2. Получить список каналов через `RoutingRepository` по `eventType`
3. Для каждого канала: получить шаблон из `TemplateRepository`, отрендерить через `TemplateEngine`
4. Отправить через `ChannelProvider`
5. Сохранить результат в `NotificationLogRepository`

### `api-adapter-kafka` — входящий Kafka-адаптер

```
api-adapter-kafka/src/main/java/com/example/notification/adapter/kafka/in/
├── NotificationEventConsumer.java    # @KafkaListener
└── NotificationEventMapper.java      # Kafka message → NotificationRequest
```

### `api-adapter-webmvc` — входящий REST-адаптер (опционально)

```
api-adapter-webmvc/src/main/java/com/example/notification/adapter/web/
├── NotificationController.java       # @RestController
└── NotificationRequestMapper.java
```

### `spi-adapter-*` — исходящие адаптеры

Каждый реализует нужные интерфейсы из `spi`:

| Адаптер | Реализует |
|---|---|
| `spi-adapter-jpa` | `TemplateRepository`, `RoutingRepository`, `NotificationLogRepository` |
| `spi-adapter-redis` | `DeduplicationPort` |
| `spi-adapter-email` | `ChannelProvider` (EMAIL) |
| `spi-adapter-push` | `ChannelProvider` (PUSH) |
| `spi-adapter-sms` | `ChannelProvider` (SMS) |
| `spi-adapter-chat` | `ChannelProvider` (CHAT) |

### `microservice` — точка запуска

```
microservice/src/main/java/com/example/notification/
└── NotificationServiceApplication.java   # @SpringBootApplication
```

Зависит от всех модулей. Содержит `application.yml` и Spring-конфигурацию.

---

## Поток обработки уведомления

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
        │         если дубликат → SKIPPED, выход
        │
        ├──▶ RoutingRepository.findChannels(eventType)
        │         → [EMAIL, PUSH]
        │
        └──▶ для каждого канала:
                  │
                  ├──▶ TemplateRepository.find(eventType, channel, locale)
                  ├──▶ TemplateEngine.render(template, templateData)
                  ├──▶ ChannelProvider.send(rendered)
                  └──▶ NotificationLogRepository.save(log)
```

---

## Контракты

### `NotificationEvent` — сообщение из Kafka

```json
{
  "eventType": "ORDER_CONFIRMED",
  "idempotencyKey": "ord-456-confirmed",
  "locale": "ru",
  "recipient": {
    "userId": "u-123",
    "email": "user@example.com",
    "phoneNumber": "+77001234567",
    "deviceTokens": ["fcm-token-abc"]
  },
  "templateData": {
    "orderId": "ORD-456",
    "amount": "4200",
    "currency": "KZT"
  }
}
```

### `ChannelProvider` — интерфейс отправки

```java
public interface ChannelProvider {
    NotificationChannel getChannel();
    NotificationResult send(RenderedNotification notification);
}
```

### `NotificationUseCase` — входной порт

```java
public interface NotificationUseCase {
    NotificationResult handle(NotificationRequest request);
}
```

---

## Схема базы данных

### `notification_routing`

```sql
CREATE TABLE notification_routing (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type  VARCHAR(100) NOT NULL,
    channel     VARCHAR(20)  NOT NULL,   -- EMAIL, PUSH, SMS, CHAT
    enabled     BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),

    UNIQUE (event_type, channel)
);
```

### `notification_template`

```sql
CREATE TABLE notification_template (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type  VARCHAR(100) NOT NULL,
    channel     VARCHAR(20)  NOT NULL,
    locale      VARCHAR(10)  NOT NULL DEFAULT 'ru',
    subject     TEXT,
    body        TEXT         NOT NULL,   -- Freemarker-шаблон
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),

    UNIQUE (event_type, channel, locale)
);
```

### `notification_log`

```sql
CREATE TABLE notification_log (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key  VARCHAR(255) NOT NULL,
    event_type       VARCHAR(100) NOT NULL,
    channel          VARCHAR(20)  NOT NULL,
    recipient_ref    VARCHAR(255),        -- email или userId
    status           VARCHAR(20)  NOT NULL,   -- SENT, FAILED, SKIPPED, PENDING
    error_message    TEXT,
    created_at       TIMESTAMP    NOT NULL DEFAULT now()
) PARTITION BY RANGE (created_at);

CREATE INDEX ON notification_log (idempotency_key);
CREATE INDEX ON notification_log (event_type, created_at);
```

---

## Конфигурация

`microservice/src/main/resources/application.yml`:

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: notification-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.example.notification.*"

  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/notifications}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

notification:
  kafka:
    topic: notifications
    dlq-topic: notifications-dlq

  deduplication:
    ttl-seconds: 86400   # 24 часа

  channels:
    email:
      provider: sendgrid
      api-key: ${SENDGRID_API_KEY}
    sms:
      provider: twilio
      account-sid: ${TWILIO_ACCOUNT_SID}
      auth-token: ${TWILIO_AUTH_TOKEN}
    push:
      fcm-server-key: ${FCM_SERVER_KEY}
```

---

## Технологический стек

| Компонент | Технология |
|---|---|
| Язык | Java 21 |
| Фреймворк | Spring Boot 3.x |
| Очередь | Apache Kafka |
| БД | PostgreSQL + Spring Data JPA |
| Кэш / Dedup | Redis (Spring Data Redis) |
| Шаблонизатор | Freemarker |
| Unit-тесты | JUnit 5 + Mockito |
| Email | SendGrid / AWS SES |
| Push | FCM + APNs (firebase-admin) |
| SMS | Twilio |
| Chat | Slack Webhooks / Telegram Bot API |
| Сборка | Maven (multi-module) |
| Контейнеризация | Docker + docker-compose |
