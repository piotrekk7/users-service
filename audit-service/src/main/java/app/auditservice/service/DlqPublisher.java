package app.auditservice.service;

import app.auditservice.dto.AuditEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DlqPublisher {

    private static final String DLQ_TOPIC = "audit.events.dlq";

    private final KafkaTemplate<String, AuditEventDto> auditDlqKafkaTemplate;

    public void sendToDlq(AuditEventDto event, Throwable throwable) {
        try {
            log.error("Sending event to DLQ: eventId={}, action={}, userId={}, error={}",
                    event.getEventId(), event.getAction(), event.getUserId(), throwable.getMessage());

            auditDlqKafkaTemplate.send(DLQ_TOPIC, event.getUserId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Event sent to DLQ successfully: eventId={}", event.getEventId());
                        } else {
                            log.error("Failed to send event to DLQ: eventId={}", event.getEventId(), ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Error sending event to DLQ: eventId={}", event.getEventId(), e);
        }
    }
}
