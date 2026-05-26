package kz.dev.notification.core.domain;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class Recipient {

    String userId;
    String email;
    String phoneNumber;

    @Builder.Default
    List<String> deviceTokens = List.of();

    public String ref() {
        if (email != null) return email;
        return userId;
    }
}
