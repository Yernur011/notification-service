package kz.dev.api.notification.dto;

import kz.dev.core.notification.domain.NotificationLog;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class NotificationLogPage {

    List<NotificationLog> entries;
    String nextLastId;
    boolean hasNext;
}
