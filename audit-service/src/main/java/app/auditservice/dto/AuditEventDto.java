package app.auditservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditEventDto {
    private String eventId;
    private String userId;
    private String action;
    private Instant timestamp;
    private String ipAddress;
    private String userAgent;
    private RequestDetailsDto requestDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RequestDetailsDto {
        private String endpoint;
        private String method;
        private Map<String, Object> additionalData;
    }
}
