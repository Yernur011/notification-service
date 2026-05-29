package kz.dev.spi.notification;

public interface DeduplicationPort {

    boolean isDuplicate(String idempotencyKey);

    void markProcessed(String idempotencyKey);
}
