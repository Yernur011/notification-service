package kz.dev.notification.core.service;

import kz.dev.notification.core.domain.NotificationTemplate;
import kz.dev.notification.core.domain.RenderedNotification;
import kz.dev.notification.core.enums.NotificationChannel;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


public class TemplateEngineTest {

    private final TemplateEngine engine = new TemplateEngine();

    @Test
    public void rendersSubjectAndBody() {
        NotificationTemplate template = NotificationTemplate.builder()
                .eventType("ORDER_CONFIRMED")
                .channel(NotificationChannel.EMAIL)
                .locale("en")
                .subject("Order ${orderId} confirmed")
                .body("Total: ${amount} ${currency}")
                .build();

        RenderedNotification result = engine.render(template, Map.of(
                "orderId", "ORD-456",
                "amount", "4200",
                "currency", "KZT"
        ));

        assertThat(result.getSubject()).isEqualTo("Order ORD-456 confirmed");
        assertThat(result.getBody()).isEqualTo("Total: 4200 KZT");
        assertThat(result.getChannel()).isEqualTo(NotificationChannel.EMAIL);
    }

    @Test
    public void rendersBodyWithoutSubject() {
        NotificationTemplate template = NotificationTemplate.builder()
                .eventType("PUSH_EVENT")
                .channel(NotificationChannel.PUSH)
                .locale("en")
                .body("Hello ${name}!")
                .build();

        RenderedNotification result = engine.render(template, Map.of("name", "Alice"));

        assertThat(result.getSubject()).isNull();
        assertThat(result.getBody()).isEqualTo("Hello Alice!");
    }
}
