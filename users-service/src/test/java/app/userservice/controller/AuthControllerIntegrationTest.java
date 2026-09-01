package app.userservice.controller;

import app.userservice.dto.LoginRequest;
import app.userservice.dto.RegisterRequest;
import app.userservice.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = {"/test-schema.sql", "/test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void register_ShouldReturnCreatedWithToken_WhenValidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "newuser@example.com",
            "password123",
            "John",
            "Doe"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.token", notNullValue()))
            .andExpect(jsonPath("$.token", not(emptyString())));

        assertThat(userRepository.findByEmail("newuser@example.com")).isPresent();
    }

    @Test
    void register_ShouldReturnBadRequest_WhenEmailIsInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "invalid-email",
            "password123",
            "John",
            "Doe"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.message", containsString("Validation failed")))
            .andExpect(jsonPath("$.errors", notNullValue()));
    }

    @Test
    void register_ShouldReturnBadRequest_WhenEmailIsMissing() throws Exception {
        RegisterRequest request = new RegisterRequest(
            null,
            "password123",
            "John",
            "Doe"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.message", containsString("Validation failed")));
    }

    @Test
    void register_ShouldReturnBadRequest_WhenPasswordIsTooShort() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "user@example.com",
            "short",
            "John",
            "Doe"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.message", containsString("Validation failed")));
    }

    @Test
    void register_ShouldReturnBadRequest_WhenFirstNameIsMissing() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "user@example.com",
            "password123",
            null,
            "Doe"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.message", containsString("Validation failed")));
    }

    @Test
    void register_ShouldReturnBadRequest_WhenLastNameIsMissing() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "user@example.com",
            "password123",
            "John",
            null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.message", containsString("Validation failed")));
    }

    @Test
    void register_ShouldReturnConflict_WhenEmailAlreadyExists() throws Exception {
        RegisterRequest firstRequest = new RegisterRequest(
            "duplicate@example.com",
            "password123",
            "John",
            "Doe"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
            .andExpect(status().isCreated());

        RegisterRequest secondRequest = new RegisterRequest(
            "duplicate@example.com",
            "password456",
            "Jane",
            "Smith"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondRequest)))
            .andExpect(status().isConflict())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status", is(409)))
            .andExpect(jsonPath("$.message", containsString("Email already in use")));
    }

    @Test
    void login_ShouldReturnOkWithToken_WhenCredentialsAreValid() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
            "loginuser@example.com",
            "password123",
            "John",
            "Doe"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(
            "loginuser@example.com",
            "password123"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.token", notNullValue()))
            .andExpect(jsonPath("$.token", not(emptyString())));
    }

    @Test
    void login_ShouldReturnUnauthorized_WhenEmailDoesNotExist() throws Exception {
        LoginRequest loginRequest = new LoginRequest(
            "nonexistent@example.com",
            "password123"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status", is(401)))
            .andExpect(jsonPath("$.message", containsString("Invalid email or password")));
    }

    @Test
    void login_ShouldReturnUnauthorized_WhenPasswordIsIncorrect() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
            "loginuser2@example.com",
            "password123",
            "John",
            "Doe"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(
            "loginuser2@example.com",
            "wrongpassword"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status", is(401)))
            .andExpect(jsonPath("$.message", containsString("Invalid email or password")));
    }

    @Test
    void login_ShouldReturnBadRequest_WhenEmailIsInvalid() throws Exception {
        LoginRequest loginRequest = new LoginRequest(
            "invalid-email",
            "password123"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.message", containsString("Validation failed")));
    }

    @Test
    void login_ShouldReturnBadRequest_WhenEmailIsMissing() throws Exception {
        LoginRequest loginRequest = new LoginRequest(null, "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.message", containsString("Validation failed")));
    }

    @Test
    void login_ShouldReturnBadRequest_WhenPasswordIsMissing() throws Exception {
        LoginRequest loginRequest = new LoginRequest("user@example.com", null);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.message", containsString("Validation failed")));
    }
}
