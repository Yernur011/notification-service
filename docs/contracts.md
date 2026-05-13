# Contracts

## Inbound: `NotificationUseCase` (api port)

```java
public interface NotificationUseCase {
    NotificationResult handle(NotificationRequest request);
}
```

## Inbound: Kafka event schema

Topic: `notifications`

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

| Field            | Required | Description                                      |
|------------------|----------|--------------------------------------------------|
| `eventType`      | yes      | Determines routing rules and template selection  |
| `idempotencyKey` | yes      | Used for deduplication (TTL: 24 hours in Redis)  |
| `locale`         | no       | Defaults to `ru`                                 |
| `recipient`      | yes      | At least one of email / phoneNumber / deviceTokens |
| `templateData`   | no       | Free-form map passed to Freemarker template       |

## Outbound: `ChannelProvider` (spi port)

```java
public interface ChannelProvider {
    NotificationChannel getChannel();
    NotificationResult send(RenderedNotification notification);
}
```

One implementation per channel (`EMAIL`, `PUSH`, `SMS`, `CHAT`). The use case selects the correct provider via `Map<NotificationChannel, ChannelProvider>`.

## Outbound: other spi ports

| Interface                    | Responsibility                                     |
|------------------------------|----------------------------------------------------|
| `TemplateRepository`         | Load Freemarker template by eventType + channel + locale |
| `RoutingRepository`          | Return enabled channels for a given eventType      |
| `NotificationLogRepository`  | Persist send result (SENT / FAILED / SKIPPED)      |
| `DeduplicationPort`          | Check and mark an idempotency key as processed     |
