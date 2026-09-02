package app.userservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private String userId;
    private String action;

    @Builder.Default
    private Instant timestamp = Instant.now();

    private String ipAddress;
    private String userAgent;
    private RequestDetails requestDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RequestDetails {
        private String endpoint;
        private String method;
        private Map<String, Object> additionalData;
    }
}
