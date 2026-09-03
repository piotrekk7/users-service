package app.auditservice.listener;

import app.auditservice.dto.AuditEventDto;
import app.auditservice.model.AuditLog;
import app.auditservice.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

@SpringBootTest
@Testcontainers
class AuditEventListenerIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Container
    static MongoDBContainer mongodb = new MongoDBContainer(DockerImageName.parse("mongo:7-jammy"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
    }

    @Autowired
    private AuditLogRepository auditLogRepository;

    private KafkaTemplate<String, AuditEventDto> testKafkaTemplate;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        ProducerFactory<String, AuditEventDto> producerFactory = new DefaultKafkaProducerFactory<>(props);
        testKafkaTemplate = new KafkaTemplate<>(producerFactory);
    }

    @AfterEach
    void tearDown() {
        auditLogRepository.deleteAll();
    }

    @Test
    void shouldConsumeValidEventAndSaveToMongoDB() {
        AuditEventDto event = AuditEventDto.builder()
                .eventId(UUID.randomUUID().toString())
                .userId("123")
                .action("USER_LOGIN_SUCCESS")
                .timestamp(Instant.now())
                .ipAddress("192.168.1.100")
                .userAgent("Mozilla/5.0")
                .requestDetails(AuditEventDto.RequestDetailsDto.builder()
                        .endpoint("/api/v1/auth/login")
                        .method("POST")
                        .build())
                .build();

        testKafkaTemplate.send("audit.events", event.getUserId(), event);

        await().atMost(10, SECONDS).untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getEventId()).isEqualTo(event.getEventId());
            assertThat(logs.get(0).getUserId()).isEqualTo("123");
            assertThat(logs.get(0).getAction()).isEqualTo("USER_LOGIN_SUCCESS");
            assertThat(logs.get(0).getIpAddress()).isEqualTo("192.168.1.100");
            assertThat(logs.get(0).getRequestDetails().getEndpoint()).isEqualTo("/api/v1/auth/login");
        });
    }

    @Test
    void shouldConsumeMultipleEventsAndSaveAll() {
        for (int i = 0; i < 10; i++) {
            AuditEventDto event = AuditEventDto.builder()
                    .eventId(UUID.randomUUID().toString())
                    .userId("user-" + i)
                    .action("USER_REGISTERED")
                    .timestamp(Instant.now())
                    .ipAddress("192.168.1." + i)
                    .userAgent("TestAgent")
                    .build();

            testKafkaTemplate.send("audit.events", event.getUserId(), event);
        }

        await().atMost(15, SECONDS).untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll();
            assertThat(logs).hasSize(10);
        });
    }

    @Test
    void shouldHandleEventWithoutRequestDetails() {
        AuditEventDto event = AuditEventDto.builder()
                .eventId(UUID.randomUUID().toString())
                .userId("456")
                .action("USER_DELETED")
                .timestamp(Instant.now())
                .ipAddress("192.168.1.200")
                .userAgent("DeleteAgent")
                .requestDetails(null)
                .build();

        testKafkaTemplate.send("audit.events", event.getUserId(), event);

        await().atMost(10, SECONDS).untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getUserId()).isEqualTo("456");
            assertThat(logs.get(0).getAction()).isEqualTo("USER_DELETED");
            assertThat(logs.get(0).getRequestDetails()).isNull();
        });
    }
}
