package app.auditservice.service;

import app.auditservice.dto.AuditEventDto;
import app.auditservice.model.AuditAction;
import app.auditservice.model.AuditLog;
import app.auditservice.model.RequestDetails;
import app.auditservice.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLog saveAuditLog(AuditEventDto eventDto) {
        log.debug("Saving audit log: eventId={}, action={}, userId={}",
                eventDto.getEventId(), eventDto.getAction(), eventDto.getUserId());

        RequestDetails requestDetails = null;
        if (eventDto.getRequestDetails() != null) {
            requestDetails = RequestDetails.builder()
                    .endpoint(eventDto.getRequestDetails().getEndpoint())
                    .method(eventDto.getRequestDetails().getMethod())
                    .additionalData(eventDto.getRequestDetails().getAdditionalData())
                    .build();
        }

        AuditAction action = AuditAction.valueOf(eventDto.getAction());

        AuditLog auditLog = AuditLog.builder()
                .eventId(eventDto.getEventId())
                .userId(eventDto.getUserId())
                .action(action)
                .timestamp(eventDto.getTimestamp())
                .ipAddress(eventDto.getIpAddress())
                .userAgent(eventDto.getUserAgent())
                .requestDetails(requestDetails)
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("Audit log saved successfully: id={}, eventId={}, action={}",
                saved.getId(), saved.getEventId(), saved.getAction());

        return saved;
    }

    public Page<AuditLog> getAuditLogsByUserId(String userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }

    public Page<AuditLog> getAuditLogsByAction(AuditAction action, Pageable pageable) {
        return auditLogRepository.findByActionOrderByTimestampDesc(action, pageable);
    }

    public Page<AuditLog> getAuditLogsByTimeRange(Instant from, Instant to, Pageable pageable) {
        return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(from, to, pageable);
    }

    public Page<AuditLog> getAuditLogsByActionAndTimeRange(AuditAction action, Instant from, Instant to, Pageable pageable) {
        return auditLogRepository.findByActionAndTimestampBetweenOrderByTimestampDesc(action, from, to, pageable);
    }
}
