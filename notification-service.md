# Notification Service

Микросервис асинхронной доставки уведомлений через Email, Push, SMS и Chat-каналы.
Построен на гексагональной архитектуре, Spring Boot, Kafka. Разрабатывается по BDD-подходу.

---

## Содержание

1. [Архитектурный обзор](#архитектурный-обзор)
2. [Структура модулей](#структура-модулей)
3. [Модули подробно](#модули-подробно)
4. [Поток обработки уведомления](#поток-обработки-уведомления)
5. [Контракты](#контракты)
6. [BDD-подход: как мы пишем код](#bdd-подход-как-мы-пишем-код)
7. [Пример: полный BDD-цикл](#пример-полный-bdd-цикл)
8. [Схема базы данных](#схема-базы-данных)
9. [Конфигурация](#конфигурация)
10. [Технологический стек](#технологический-стек)

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
└── service/
    └── TemplateEngine.java            # рендеринг шаблона (Freemarker)
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

## BDD-подход: как мы пишем код

Используем **Cucumber + JUnit 5** для описания поведения на человекочитаемом языке (Gherkin).
Каждый use case описывается сценарием **до** написания кода.

### Цикл разработки (BDD)

```
1. Написать .feature файл (Gherkin сценарий)
        ↓
2. Запустить тест → он красный (нет реализации)
        ↓
3. Написать минимальный код чтобы тест стал зелёным
        ↓
4. Рефакторинг
        ↓
5. Следующий сценарий
```

### Структура тестов

```
usecase/src/test/
├── java/com/example/notification/usecase/
│   ├── steps/
│   │   ├── SendNotificationSteps.java     # step definitions
│   │   └── SharedTestContext.java         # общий контекст сценария
│   └── config/
│       └── CucumberSpringConfig.java
└── resources/
    └── features/
        ├── send_notification.feature
        ├── deduplication.feature
        ├── routing.feature
        └── template_rendering.feature
```

### Зависимости для BDD

```xml
<!-- pom.xml в модуле usecase -->
<dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-java</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-spring</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-junit-platform-engine</artifactId>
    <scope>test</scope>
</dependency>
```

### Правила написания сценариев

- **Feature** = один use case или бизнес-правило
- **Scenario** = конкретный случай поведения
- **Given** = начальное состояние системы
- **When** = действие (что происходит)
- **Then** = ожидаемый результат
- Сценарий читается как спецификация, понятная бизнесу

---

## Пример: полный BDD-цикл

### Шаг 1: Feature-файл

`send_notification.feature`:

```gherkin
Feature: Отправка уведомления по событию

  Background:
    Given существует routing-правило: событие "ORDER_CONFIRMED" отправляется через EMAIL и PUSH
    And существует шаблон для события "ORDER_CONFIRMED" и канала EMAIL:
      | subject | Ваш заказ {{orderId}} подтверждён          |
      | body    | Сумма заказа: {{amount}} {{currency}}      |

  Scenario: Успешная отправка уведомления
    Given получатель с email "user@example.com" и device token "fcm-abc"
    When приходит событие "ORDER_CONFIRMED" с данными:
      | orderId  | ORD-456 |
      | amount   | 4200    |
      | currency | KZT     |
    Then уведомление отправлено через EMAIL на "user@example.com"
    And уведомление отправлено через PUSH на токен "fcm-abc"
    And в логе записан статус SENT для обоих каналов

  Scenario: Дубликат события игнорируется
    Given событие с idempotencyKey "ord-456-confirmed" уже было обработано
    When приходит повторное событие "ORDER_CONFIRMED" с тем же idempotencyKey
    Then уведомление НЕ отправляется
    And в логе записан статус SKIPPED

  Scenario: Отправка через EMAIL падает — PUSH продолжает работать
    Given Email-провайдер недоступен
    When приходит событие "ORDER_CONFIRMED"
    Then уведомление отправлено через PUSH
    And в логе записан статус FAILED для EMAIL и SENT для PUSH
```

### Шаг 2: Step Definitions

```java
@CucumberContextConfiguration
@SpringBootTest
public class SendNotificationSteps {

    @Autowired
    private NotificationUseCase notificationUseCase;

    // Моки через Mockito или тестовые реализации (test doubles)
    @MockBean private ChannelProvider emailProvider;
    @MockBean private ChannelProvider pushProvider;
    @MockBean private RoutingRepository routingRepository;
    @MockBean private TemplateRepository templateRepository;
    @MockBean private DeduplicationPort deduplicationPort;
    @MockBean private NotificationLogRepository logRepository;

    private NotificationRequest request;

    @Given("существует routing-правило: событие {string} отправляется через EMAIL и PUSH")
    public void existsRoutingRule(String eventType) {
        when(routingRepository.findChannels(eventType))
            .thenReturn(List.of(EMAIL, PUSH));
    }

    @Given("существует шаблон для события {string} и канала EMAIL:")
    public void existsEmailTemplate(String eventType, DataTable table) {
        Map<String, String> data = table.asMap();
        var template = NotificationTemplate.builder()
            .eventType(eventType)
            .channel(EMAIL)
            .subject(data.get("subject"))
            .body(data.get("body"))
            .build();
        when(templateRepository.find(eventType, EMAIL, "ru"))
            .thenReturn(Optional.of(template));
    }

    @When("приходит событие {string} с данными:")
    public void eventArrives(String eventType, DataTable table) {
        request = NotificationRequest.builder()
            .eventType(eventType)
            .idempotencyKey("test-key-" + eventType)
            .locale("ru")
            .recipient(Recipient.builder()
                .email("user@example.com")
                .deviceTokens(List.of("fcm-abc"))
                .build())
            .templateData(table.asMap())
            .build();

        notificationUseCase.handle(request);
    }

    @Then("уведомление отправлено через EMAIL на {string}")
    public void notificationSentViaEmail(String email) {
        verify(emailProvider).send(argThat(n ->
            n.getChannel() == EMAIL &&
            n.getRecipient().getEmail().equals(email)
        ));
    }

    @Then("в логе записан статус SENT для обоих каналов")
    public void logContainsSentForBothChannels() {
        verify(logRepository, times(2)).save(argThat(log ->
            log.getStatus() == SENT
        ));
    }

    @Then("уведомление НЕ отправляется")
    public void notificationNotSent() {
        verifyNoInteractions(emailProvider, pushProvider);
    }
}
```

### Шаг 3: Реализация `SendNotificationUseCase`

Пишем минимальный код, чтобы сценарии стали зелёными:

```java
@Component
@RequiredArgsConstructor
public class SendNotificationUseCase implements NotificationUseCase {

    private final DeduplicationPort deduplicationPort;
    private final RoutingRepository routingRepository;
    private final TemplateRepository templateRepository;
    private final TemplateEngine templateEngine;
    private final Map<NotificationChannel, ChannelProvider> channelProviders;
    private final NotificationLogRepository logRepository;

    @Override
    public NotificationResult handle(NotificationRequest request) {

        if (deduplicationPort.isDuplicate(request.getIdempotencyKey())) {
            logRepository.save(NotificationLog.skipped(request));
            return NotificationResult.skipped();
        }

        deduplicationPort.markProcessed(request.getIdempotencyKey());

        List<NotificationChannel> channels =
            routingRepository.findChannels(request.getEventType());

        for (NotificationChannel channel : channels) {
            NotificationLog log = NotificationLog.pending(request, channel);
            try {
                var template = templateRepository
                    .find(request.getEventType(), channel, request.getLocale())
                    .orElseThrow(() -> new TemplateNotFoundException(request.getEventType(), channel));

                var rendered = templateEngine.render(template, request.getTemplateData());
                rendered.setRecipient(request.getRecipient());
                rendered.setChannel(channel);

                channelProviders.get(channel).send(rendered);

                log.markSent();
            } catch (Exception e) {
                log.markFailed(e.getMessage());
            } finally {
                logRepository.save(log);
            }
        }

        return NotificationResult.processed();
    }
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
| BDD-тестирование | Cucumber 7 + JUnit 5 |
| Unit-тесты | JUnit 5 + Mockito |
| Email | SendGrid / AWS SES |
| Push | FCM + APNs (firebase-admin) |
| SMS | Twilio |
| Chat | Slack Webhooks / Telegram Bot API |
| Сборка | Maven (multi-module) |
| Контейнеризация | Docker + docker-compose |
