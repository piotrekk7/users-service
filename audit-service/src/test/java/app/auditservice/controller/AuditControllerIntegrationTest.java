package app.auditservice.controller;

import app.auditservice.model.AuditAction;
import app.auditservice.model.AuditLog;
import app.auditservice.model.RequestDetails;
import app.auditservice.repository.AuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuditControllerIntegrationTest {

    @Container
    static MongoDBContainer mongodb = new MongoDBContainer(DockerImageName.parse("mongo:7-jammy"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9999");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        auditLogRepository.deleteAll();
    }

    @Test
    void shouldReturnAuditLogsForUser() throws Exception {
        AuditLog log1 = createAuditLog("user-123", AuditAction.USER_LOGIN_SUCCESS, Instant.now());
        AuditLog log2 = createAuditLog("user-123", AuditAction.USER_UPDATED, Instant.now().plus(1, ChronoUnit.HOURS));
        AuditLog log3 = createAuditLog("user-456", AuditAction.USER_REGISTERED, Instant.now());

        auditLogRepository.saveAll(List.of(log1, log2, log3));

        mockMvc.perform(get("/api/v1/audit/user/user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].userId").value("user-123"))
                .andExpect(jsonPath("$.data[0].action").value(AuditAction.USER_UPDATED))
                .andExpect(jsonPath("$.data[1].action").value(AuditAction.USER_LOGIN_SUCCESS))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void shouldReturnEmptyListForNonExistentUser() throws Exception {
        mockMvc.perform(get("/api/v1/audit/user/non-existent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void shouldReturnAuditLogsByAction() throws Exception {
        AuditLog log1 = createAuditLog("user-123", AuditAction.USER_LOGIN_SUCCESS, Instant.now());
        AuditLog log2 = createAuditLog("user-456", AuditAction.USER_LOGIN_SUCCESS, Instant.now().plus(1, ChronoUnit.HOURS));
        AuditLog log3 = createAuditLog("user-789", AuditAction.USER_REGISTERED, Instant.now());

        auditLogRepository.saveAll(List.of(log1, log2, log3));

        mockMvc.perform(get("/api/v1/audit")
                        .param("action", AuditAction.USER_LOGIN_SUCCESS.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].action").value("USER_LOGIN_SUCCESS"))
                .andExpect(jsonPath("$.data[1].action").value("USER_LOGIN_SUCCESS"))
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void shouldReturnAuditLogsByTimeRange() throws Exception {
        Instant baseTime = Instant.now();
        AuditLog log1 = createAuditLog("user-123", AuditAction.USER_REGISTERED, baseTime.minus(2, ChronoUnit.DAYS));
        AuditLog log2 = createAuditLog("user-456", AuditAction.USER_LOGIN_SUCCESS, baseTime.minus(1, ChronoUnit.DAYS));
        AuditLog log3 = createAuditLog("user-789", AuditAction.USER_UPDATED, baseTime);

        auditLogRepository.saveAll(List.of(log1, log2, log3));

        String from = baseTime.minus(2, ChronoUnit.DAYS).toString();
        String to = baseTime.minus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS).toString();

        mockMvc.perform(get("/api/v1/audit")
                        .param("from", from)
                        .param("to", to))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void shouldRespectPaginationParameters() throws Exception {
        for (int i = 0; i < 25; i++) {
            AuditLog log = createAuditLog("user-123", AuditAction.USER_LOGIN_SUCCESS, Instant.now().plus(i, ChronoUnit.MINUTES));
            auditLogRepository.save(log);
        }

        mockMvc.perform(get("/api/v1/audit/user/user-123")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(10)))
                .andExpect(jsonPath("$.total").value(25))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));

        mockMvc.perform(get("/api/v1/audit/user/user-123")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(10)))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void shouldEnforceMaxPageSize() throws Exception {
        for (int i = 0; i < 150; i++) {
            AuditLog log = createAuditLog("user-123", AuditAction.USER_LOGIN_SUCCESS, Instant.now().plus(i, ChronoUnit.MINUTES));
            auditLogRepository.save(log);
        }

        mockMvc.perform(get("/api/v1/audit/user/user-123")
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void shouldReturnBadRequestWhenNoParametersProvided() throws Exception {
        mockMvc.perform(get("/api/v1/audit"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Either 'action' or both 'from' and 'to' parameters are required"));
    }

    private AuditLog createAuditLog(String userId, AuditAction action, Instant timestamp) {
        return AuditLog.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(userId)
                .action(action)
                .timestamp(timestamp)
                .ipAddress("192.168.1.100")
                .userAgent("TestAgent")
                .requestDetails(RequestDetails.builder()
                        .endpoint("/api/test")
                        .method("POST")
                        .build())
                .build();
    }
}
