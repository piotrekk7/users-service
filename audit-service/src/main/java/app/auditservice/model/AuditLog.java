package app.auditservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_logs")
@CompoundIndex(name = "userId_timestamp_idx", def = "{'userId': 1, 'timestamp': -1}")
public class AuditLog {
    @Id
    private String id;

    private String eventId;

    @Indexed
    private String userId;

    @Indexed
    private AuditAction action;

    @Indexed
    private Instant timestamp;

    private String ipAddress;

    private String userAgent;

    private RequestDetails requestDetails;
}
