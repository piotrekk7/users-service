package app.auditservice.listener;

import app.auditservice.dto.AuditEventDto;
import app.auditservice.service.AuditLogService;
import app.auditservice.service.DlqPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventListener {

    private final AuditLogService auditLogService;
    private final DlqPublisher dlqPublisher;
    private final RetryTemplate retryTemplate;

    @KafkaListener(
            topics = "audit.events",
            groupId = "audit-service-group",
            concurrency = "3",
            containerFactory = "auditKafkaListenerContainerFactory"
    )
    public void listen(
            @Payload AuditEventDto event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            Acknowledgment acknowledgment
    ) {
        log.debug("Received audit event from partition {}: eventId={}, action={}, userId={}",
                partition, event.getEventId(), event.getAction(), event.getUserId());

        try {
            retryTemplate.execute(context -> {
                if (context.getRetryCount() > 0) {
                    log.warn("Retry attempt {} for eventId={}", context.getRetryCount(), event.getEventId());
                }
                auditLogService.saveAuditLog(event);
                return null;
            }, context -> {
                log.error("All retry attempts failed for eventId={}, sending to DLQ", event.getEventId());
                dlqPublisher.sendToDlq(event, context.getLastThrowable());
                return null;
            });

            acknowledgment.acknowledge();
            log.debug("Event processed and acknowledged: eventId={}", event.getEventId());

        } catch (Exception e) {
            log.error("Unexpected error processing event: eventId={}", event.getEventId(), e);
            dlqPublisher.sendToDlq(event, e);
            acknowledgment.acknowledge();
        }
    }
}
