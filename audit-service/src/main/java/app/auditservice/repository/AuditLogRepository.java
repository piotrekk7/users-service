package app.auditservice.repository;

import app.auditservice.model.AuditAction;
import app.auditservice.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    List<AuditLog> findByUserIdOrderByTimestampDesc(String userId);
    Page<AuditLog> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);
    Page<AuditLog> findByActionOrderByTimestampDesc(AuditAction action, Pageable pageable);
    Page<AuditLog> findByTimestampBetweenOrderByTimestampDesc(Instant from, Instant to, Pageable pageable);
    Page<AuditLog> findByActionAndTimestampBetweenOrderByTimestampDesc(AuditAction action, Instant from, Instant to, Pageable pageable);
}
