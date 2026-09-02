package app.userservice.audit;

import app.userservice.dto.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditEventPublisher {

    private static final String AUDIT_TOPIC = "audit.events";

    private final KafkaTemplate<String, AuditEvent> auditKafkaTemplate;

    @Async("auditTaskExecutor")
    public void publishAuditEvent(AuditEvent auditEvent) {
        try {
            auditKafkaTemplate.send(AUDIT_TOPIC, auditEvent.getUserId(), auditEvent)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Audit event published successfully: eventId={}, action={}, userId={}",
                                    auditEvent.getEventId(), auditEvent.getAction(), auditEvent.getUserId());
                        } else {
                            log.error("Failed to publish audit event: eventId={}, action={}, userId={}",
                                    auditEvent.getEventId(), auditEvent.getAction(), auditEvent.getUserId(), ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing audit event: eventId={}", auditEvent.getEventId(), e);
        }
    }
}
