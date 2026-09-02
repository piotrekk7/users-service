package app.userservice.audit;

import app.userservice.dto.AuditEvent;
import app.userservice.dto.LoginRequest;
import app.userservice.dto.RegisterRequest;
import app.userservice.dto.UpdateUserRequest;
import app.userservice.model.User;
import app.userservice.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for AuditEvent publishing to Kafka.
 *
 * IMPORTANT: This test requires Kafka to be running locally.
 *
 * To run this test:
 * 1. Start services with docker-compose: docker-compose up -d
 * 2. Add to /etc/hosts: 127.0.0.1 kafka
 * 3. Run with: mvn test -Dtest=AuditEventPublisherIntegrationTest
 *
 * This test is @Disabled by default. For regular unit tests without Kafka, see AuditEventPublisherTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Disabled("Requires running Kafka instance. Use AuditEventPublisherTest for unit tests without Kafka.")
@Sql(scripts = {"/test-schema.sql", "/test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AuditEventPublisherIntegrationTest {

    private static final String AUDIT_TOPIC = "audit.events";
    private static final String KAFKA_BOOTSTRAP_SERVERS = "localhost:9092";

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", () -> KAFKA_BOOTSTRAP_SERVERS);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private BlockingQueue<ConsumerRecord<String, AuditEvent>> records;
    private KafkaMessageListenerContainer<String, AuditEvent> container;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // Setup Kafka consumer to verify published events
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVERS);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "app.userservice.dto");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, AuditEvent.class.getName());

        DefaultKafkaConsumerFactory<String, AuditEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties(AUDIT_TOPIC);

        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        records = new LinkedBlockingQueue<>();

        container.setupMessageListener((MessageListener<String, AuditEvent>) records::add);
        container.start();

        // Wait for container to start instead of waiting for specific partition assignment
        try {
            Thread.sleep(3000); // Give Kafka time to assign partitions
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.stop();
        }
        userRepository.deleteAll();
    }

    @Test
    void register_ShouldPublishUserRegisteredEvent() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "newuser@example.com",
                "password123",
                "John",
                "Doe"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        ConsumerRecord<String, AuditEvent> record = records.poll(30, TimeUnit.SECONDS);
        assertThat(record).isNotNull();

        AuditEvent event = record.value();
        assertThat(event.getAction()).isEqualTo("USER_REGISTERED");
        assertThat(event.getUserId()).isNotNull();
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getIpAddress()).isNotNull();
        assertThat(event.getUserAgent()).isNotNull();
        assertThat(event.getRequestDetails()).isNotNull();
        assertThat(event.getRequestDetails().getEndpoint()).isEqualTo("/api/v1/auth/register");
        assertThat(event.getRequestDetails().getMethod()).isEqualTo("POST");
    }

    @Test
    void login_ShouldPublishUserLoginSuccessEvent_WhenCredentialsAreValid() throws Exception {
        // First register a user
        RegisterRequest registerRequest = new RegisterRequest(
                "user@example.com",
                "password123",
                "Jane",
                "Doe"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Consume the register event
        records.poll(5, TimeUnit.SECONDS);

        // Now login
        LoginRequest loginRequest = new LoginRequest("user@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        ConsumerRecord<String, AuditEvent> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();

        AuditEvent event = record.value();
        assertThat(event.getAction()).isEqualTo("USER_LOGIN_SUCCESS");
        assertThat(event.getUserId()).isNotNull();
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getIpAddress()).isNotNull();
        assertThat(event.getRequestDetails().getEndpoint()).isEqualTo("/api/v1/auth/login");
        assertThat(event.getRequestDetails().getMethod()).isEqualTo("POST");
    }

    @Test
    void login_ShouldPublishUserLoginFailedEvent_WhenPasswordIsInvalid() throws Exception {
        // First register a user
        RegisterRequest registerRequest = new RegisterRequest(
                "user2@example.com",
                "password123",
                "Bob",
                "Smith"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Consume the register event
        records.poll(5, TimeUnit.SECONDS);

        // Try to login with wrong password
        LoginRequest loginRequest = new LoginRequest("user2@example.com", "wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());

        ConsumerRecord<String, AuditEvent> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();

        AuditEvent event = record.value();
        assertThat(event.getAction()).isEqualTo("USER_LOGIN_FAILED");
        assertThat(event.getUserId()).isNotNull();
        assertThat(event.getRequestDetails().getEndpoint()).isEqualTo("/api/v1/auth/login");
    }

    @Test
    void login_ShouldPublishUserLoginFailedEvent_WhenEmailNotFound() throws Exception {
        LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());

        ConsumerRecord<String, AuditEvent> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();

        AuditEvent event = record.value();
        assertThat(event.getAction()).isEqualTo("USER_LOGIN_FAILED");
        assertThat(event.getUserId()).isEqualTo("unknown");
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void updateUser_ShouldPublishUserUpdatedEvent() throws Exception {
        // Create a user first
        RegisterRequest registerRequest = new RegisterRequest(
                "update@example.com",
                "password123",
                "Update",
                "User"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Consume the register event
        records.poll(5, TimeUnit.SECONDS);

        User user = userRepository.findByEmail("update@example.com").orElseThrow();

        // Update the user
        UpdateUserRequest updateRequest = new UpdateUserRequest(
                "updated@example.com",
                "UpdatedFirst",
                "UpdatedLast",
                user.getRole().getId()
        );

        mockMvc.perform(put("/api/v1/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        ConsumerRecord<String, AuditEvent> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();

        AuditEvent event = record.value();
        assertThat(event.getAction()).isEqualTo("USER_UPDATED");
        assertThat(event.getUserId()).isEqualTo(user.getId().toString());
        assertThat(event.getRequestDetails().getEndpoint()).isEqualTo("/api/v1/users/" + user.getId());
        assertThat(event.getRequestDetails().getMethod()).isEqualTo("PUT");
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void deleteUser_ShouldPublishUserDeletedEvent() throws Exception {
        // Create a user first
        RegisterRequest registerRequest = new RegisterRequest(
                "delete@example.com",
                "password123",
                "Delete",
                "User"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Consume the register event
        records.poll(5, TimeUnit.SECONDS);

        User user = userRepository.findByEmail("delete@example.com").orElseThrow();

        // Delete the user
        mockMvc.perform(delete("/api/v1/users/" + user.getId()))
                .andExpect(status().isNoContent());

        ConsumerRecord<String, AuditEvent> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();

        AuditEvent event = record.value();
        assertThat(event.getAction()).isEqualTo("USER_DELETED");
        assertThat(event.getUserId()).isEqualTo(user.getId().toString());
        assertThat(event.getRequestDetails().getEndpoint()).isEqualTo("/api/v1/users/" + user.getId());
        assertThat(event.getRequestDetails().getMethod()).isEqualTo("DELETE");
    }
}
