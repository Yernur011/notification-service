# BDD Guide

The project follows a **BDD (Behaviour-Driven Development)** approach using Cucumber 7 + JUnit 5. Every use case is described in a `.feature` file before any implementation is written.

## Development cycle

```
1. Write a .feature file (Gherkin scenario)
        ↓
2. Run the test → RED (no implementation yet)
        ↓
3. Write the minimum code to make the test GREEN
        ↓
4. Refactor
        ↓
5. Next scenario
```

## Test structure

```
usecase/src/test/
├── java/com/example/notification/usecase/
│   ├── steps/
│   │   ├── SendNotificationSteps.java     # step definitions
│   │   └── SharedTestContext.java         # shared scenario context
│   └── config/
│       └── CucumberSpringConfig.java
└── resources/
    └── features/
        ├── send_notification.feature
        ├── deduplication.feature
        ├── routing.feature
        └── template_rendering.feature
```

## Gherkin conventions

- **Feature** = one use case or business rule
- **Scenario** = one concrete behaviour case
- **Given** = initial system state
- **When** = the action that occurs
- **Then** = expected outcome
- Scenarios should read as plain business specifications

## Maven dependencies (usecase module)

```xml
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

## Example: full BDD cycle

### Step 1 — Feature file (`send_notification.feature`)

```gherkin
Feature: Send notification on event

  Background:
    Given a routing rule exists: event "ORDER_CONFIRMED" is delivered via EMAIL and PUSH
    And an EMAIL template exists for event "ORDER_CONFIRMED":
      | subject | Your order {{orderId}} is confirmed       |
      | body    | Order total: {{amount}} {{currency}}      |

  Scenario: Successful notification delivery
    Given a recipient with email "user@example.com" and device token "fcm-abc"
    When an "ORDER_CONFIRMED" event arrives with data:
      | orderId  | ORD-456 |
      | amount   | 4200    |
      | currency | KZT     |
    Then a notification is sent via EMAIL to "user@example.com"
    And a notification is sent via PUSH to token "fcm-abc"
    And the log contains status SENT for both channels

  Scenario: Duplicate event is ignored
    Given an event with idempotencyKey "ord-456-confirmed" was already processed
    When the same "ORDER_CONFIRMED" event arrives again with the same idempotencyKey
    Then no notification is sent
    And the log contains status SKIPPED

  Scenario: EMAIL failure does not stop PUSH
    Given the Email provider is unavailable
    When an "ORDER_CONFIRMED" event arrives
    Then a notification is sent via PUSH
    And the log contains status FAILED for EMAIL and SENT for PUSH
```

### Step 2 — Step definitions

```java
@CucumberContextConfiguration
@SpringBootTest
public class SendNotificationSteps {

    @Autowired
    private NotificationUseCase notificationUseCase;

    @MockBean private ChannelProvider emailProvider;
    @MockBean private ChannelProvider pushProvider;
    @MockBean private RoutingRepository routingRepository;
    @MockBean private TemplateRepository templateRepository;
    @MockBean private DeduplicationPort deduplicationPort;
    @MockBean private NotificationLogRepository logRepository;

    private NotificationRequest request;

    @Given("a routing rule exists: event {string} is delivered via EMAIL and PUSH")
    public void routingRuleExists(String eventType) {
        when(routingRepository.findChannels(eventType))
            .thenReturn(List.of(EMAIL, PUSH));
    }

    @When("an {string} event arrives with data:")
    public void eventArrives(String eventType, DataTable table) {
        request = NotificationRequest.builder()
            .eventType(eventType)
            .idempotencyKey("test-key-" + eventType)
            .locale("en")
            .recipient(Recipient.builder()
                .email("user@example.com")
                .deviceTokens(List.of("fcm-abc"))
                .build())
            .templateData(table.asMap())
            .build();

        notificationUseCase.handle(request);
    }

    @Then("a notification is sent via EMAIL to {string}")
    public void notificationSentViaEmail(String email) {
        verify(emailProvider).send(argThat(n ->
            n.getChannel() == EMAIL &&
            n.getRecipient().getEmail().equals(email)
        ));
    }

    @Then("no notification is sent")
    public void noNotificationSent() {
        verifyNoInteractions(emailProvider, pushProvider);
    }
}
```

### Step 3 — Minimal implementation

Write just enough code in `SendNotificationUseCase` to make all scenarios green, then refactor.
