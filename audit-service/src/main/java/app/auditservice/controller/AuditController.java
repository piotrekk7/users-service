package app.auditservice.controller;

import app.auditservice.model.AuditAction;
import app.auditservice.model.AuditLog;
import app.auditservice.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogService auditLogService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getAuditLogsByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> auditLogs = auditLogService.getAuditLogsByUserId(userId, pageable);

        return ResponseEntity.ok(buildResponse(auditLogs));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);

        Page<AuditLog> auditLogs;

        if (action != null && from != null && to != null) {
            auditLogs = auditLogService.getAuditLogsByActionAndTimeRange(action, from, to, pageable);
        } else if (action != null) {
            auditLogs = auditLogService.getAuditLogsByAction(action, pageable);
        } else if (from != null && to != null) {
            auditLogs = auditLogService.getAuditLogsByTimeRange(from, to, pageable);
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Either 'action' or both 'from' and 'to' parameters are required"
            ));
        }

        return ResponseEntity.ok(buildResponse(auditLogs));
    }

    private Map<String, Object> buildResponse(Page<AuditLog> auditLogs) {
        Map<String, Object> response = new HashMap<>();
        response.put("data", auditLogs.getContent());
        response.put("total", auditLogs.getTotalElements());
        response.put("page", auditLogs.getNumber());
        response.put("size", auditLogs.getSize());
        return response;
    }
}
