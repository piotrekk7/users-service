package app.userservice.audit;

import app.userservice.dto.AuditEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit test for AuditEventPublisher - uses mocks, no real Kafka required.
 * This test verifies that AuditEventPublisher correctly sends events to Kafka
 * without requiring a running Kafka instance.
 */
@ExtendWith(MockitoExtension.class)
class AuditEventPublisherTest {

    private static final String AUDIT_TOPIC = "audit.events";

    @Mock
    private KafkaTemplate<String, AuditEvent> auditKafkaTemplate;

    @InjectMocks
    private AuditEventPublisher auditEventPublisher;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<AuditEvent> eventCaptor;

    private CompletableFuture<SendResult<String, AuditEvent>> successFuture;

    @BeforeEach
    void setUp() {
        // Mock successful Kafka send (lenient to allow override in specific tests)
        successFuture = CompletableFuture.completedFuture(null);
        lenient().when(auditKafkaTemplate.send(anyString(), anyString(), any(AuditEvent.class)))
                .thenReturn(successFuture);
    }

    @Test
    void publishAuditEvent_ShouldSendEventToKafka() {
        // Given
        AuditEvent event = AuditEvent.builder()
                .eventId("event-123")
                .userId("user-123")
                .action("USER_REGISTERED")
                .timestamp(Instant.now())
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .build();

        // When
        auditEventPublisher.publishAuditEvent(event);

        // Then
        verify(auditKafkaTemplate).send(
                topicCaptor.capture(),
                keyCaptor.capture(),
                eventCaptor.capture()
        );

        assertThat(topicCaptor.getValue()).isEqualTo(AUDIT_TOPIC);
        assertThat(keyCaptor.getValue()).isEqualTo("user-123");
        assertThat(eventCaptor.getValue()).isEqualTo(event);
    }

    @Test
    void publishAuditEvent_WithUnknownUser_ShouldUseUnknownAsKey() {
        // Given
        AuditEvent event = AuditEvent.builder()
                .eventId("event-456")
                .userId("unknown")
                .action("USER_LOGIN_FAILED")
                .timestamp(Instant.now())
                .ipAddress("10.0.0.1")
                .userAgent("curl/7.0")
                .build();

        // When
        auditEventPublisher.publishAuditEvent(event);

        // Then
        verify(auditKafkaTemplate).send(eq(AUDIT_TOPIC), eq("unknown"), eq(event));
    }

    @Test
    void publishAuditEvent_ShouldHandleKafkaFailureGracefully() {
        // Given
        AuditEvent event = AuditEvent.builder()
                .eventId("event-789")
                .userId("user-456")
                .action("USER_UPDATED")
                .timestamp(Instant.now())
                .build();

        // Simulate Kafka failure
        CompletableFuture<SendResult<String, AuditEvent>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka is down"));
        when(auditKafkaTemplate.send(anyString(), anyString(), any(AuditEvent.class)))
                .thenReturn(failedFuture);

        // When - should not throw exception
        auditEventPublisher.publishAuditEvent(event);

        // Then
        verify(auditKafkaTemplate).send(eq(AUDIT_TOPIC), eq("user-456"), eq(event));
    }

    @Test
    void publishAuditEvent_MultipleEvents_ShouldSendAll() {
        // Given
        AuditEvent event1 = AuditEvent.builder()
                .eventId("event-1")
                .userId("user1")
                .action("USER_REGISTERED")
                .timestamp(Instant.now())
                .build();

        AuditEvent event2 = AuditEvent.builder()
                .eventId("event-2")
                .userId("user2")
                .action("USER_LOGIN_SUCCESS")
                .timestamp(Instant.now())
                .build();

        AuditEvent event3 = AuditEvent.builder()
                .eventId("event-3")
                .userId("user3")
                .action("USER_DELETED")
                .timestamp(Instant.now())
                .build();

        // When
        auditEventPublisher.publishAuditEvent(event1);
        auditEventPublisher.publishAuditEvent(event2);
        auditEventPublisher.publishAuditEvent(event3);

        // Then
        verify(auditKafkaTemplate, times(3)).send(
                eq(AUDIT_TOPIC),
                anyString(),
                eventCaptor.capture()
        );

        assertThat(eventCaptor.getAllValues()).hasSize(3);
        assertThat(eventCaptor.getAllValues().get(0).getAction()).isEqualTo("USER_REGISTERED");
        assertThat(eventCaptor.getAllValues().get(1).getAction()).isEqualTo("USER_LOGIN_SUCCESS");
        assertThat(eventCaptor.getAllValues().get(2).getAction()).isEqualTo("USER_DELETED");
    }

    @Test
    void publishAuditEvent_ShouldUseUserIdAsKafkaKey() {
        // Given
        AuditEvent event = AuditEvent.builder()
                .eventId("event-key-test")
                .userId("specific-user-id")
                .action("USER_UPDATED")
                .timestamp(Instant.now())
                .build();

        // When
        auditEventPublisher.publishAuditEvent(event);

        // Then
        verify(auditKafkaTemplate).send(
                eq(AUDIT_TOPIC),
                keyCaptor.capture(),
                any(AuditEvent.class)
        );

        // Verify userId is used as Kafka partition key
        assertThat(keyCaptor.getValue()).isEqualTo("specific-user-id");
    }
}
