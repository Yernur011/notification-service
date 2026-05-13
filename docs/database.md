# Database Schema

Database: **PostgreSQL**. All tables live in the default schema.

## `notification_routing`

Defines which channels to use for each event type.

```sql
CREATE TABLE notification_routing (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type  VARCHAR(100) NOT NULL,
    channel     VARCHAR(20)  NOT NULL,   -- EMAIL | PUSH | SMS | CHAT
    enabled     BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),

    UNIQUE (event_type, channel)
);
```

## `notification_template`

Stores Freemarker templates per event type, channel, and locale.

```sql
CREATE TABLE notification_template (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type  VARCHAR(100) NOT NULL,
    channel     VARCHAR(20)  NOT NULL,
    locale      VARCHAR(10)  NOT NULL DEFAULT 'ru',
    subject     TEXT,                        -- used by EMAIL channel
    body        TEXT         NOT NULL,       -- Freemarker template string
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),

    UNIQUE (event_type, channel, locale)
);
```

Template variables are rendered from `NotificationEvent.templateData` using Freemarker syntax: `${orderId}`, `${amount}`, etc.

## `notification_log`

Audit log of every send attempt. Partitioned by `created_at` for scalability.

```sql
CREATE TABLE notification_log (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key  VARCHAR(255) NOT NULL,
    event_type       VARCHAR(100) NOT NULL,
    channel          VARCHAR(20)  NOT NULL,
    recipient_ref    VARCHAR(255),            -- email address or userId
    status           VARCHAR(20)  NOT NULL,   -- SENT | FAILED | SKIPPED | PENDING
    error_message    TEXT,
    created_at       TIMESTAMP    NOT NULL DEFAULT now()
) PARTITION BY RANGE (created_at);

CREATE INDEX ON notification_log (idempotency_key);
CREATE INDEX ON notification_log (event_type, created_at);
```

## Status values

| Status    | Meaning                                              |
|-----------|------------------------------------------------------|
| `PENDING` | Delivery in progress                                 |
| `SENT`    | Provider accepted the message                        |
| `FAILED`  | Provider returned an error                           |
| `SKIPPED` | Duplicate idempotency key — delivery was not retried |
