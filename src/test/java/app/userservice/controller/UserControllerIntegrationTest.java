package app.userservice.controller;

import app.userservice.dto.CreateUserRequest;
import app.userservice.dto.RegisterRequest;
import app.userservice.dto.UpdateUserRequest;
import app.userservice.model.Role;
import app.userservice.model.User;
import app.userservice.repository.RoleRepository;
import app.userservice.repository.UserRepository;
import app.userservice.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = {"/test-schema.sql", "/test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Role adminRole;
    private Role userRole;
    private User adminUser;
    private User regularUser;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        adminRole = roleRepository.findByName("ADMIN")
            .orElseThrow(() -> new IllegalStateException("ADMIN role not found"));
        userRole = roleRepository.findByName("USER")
            .orElseThrow(() -> new IllegalStateException("USER role not found"));

        adminUser = new User();
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(passwordEncoder.encode("password123"));
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setRole(adminRole);
        adminUser.setCreatedAt(LocalDateTime.now());
        adminUser = userRepository.save(adminUser);

        regularUser = new User();
        regularUser.setEmail("user@example.com");
        regularUser.setPassword(passwordEncoder.encode("password123"));
        regularUser.setFirstName("Regular");
        regularUser.setLastName("User");
        regularUser.setRole(userRole);
        regularUser.setCreatedAt(LocalDateTime.now());
        regularUser = userRepository.save(regularUser);

        adminToken = jwtTokenProvider.generateToken(adminUser);
        userToken = jwtTokenProvider.generateToken(regularUser);
    }

    @Test
    void getUsers_ShouldReturnPagedUsers_WhenAuthenticatedAsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.totalElements", is(2)))
            .andExpect(jsonPath("$.totalPages", is(1)))
            .andExpect(jsonPath("$.currentPage", is(0)))
            .andExpect(jsonPath("$.pageSize", is(10)));
    }

    @Test
    void getUsers_ShouldSupportPagination_WhenAuthenticatedAsAdmin() throws Exception {
        for (int i = 0; i < 15; i++) {
            User user = new User();
            user.setEmail("user" + i + "@example.com");
            user.setPassword(passwordEncoder.encode("password123"));
            user.setFirstName("User" + i);
            user.setLastName("Test");
            user.setRole(userRole);
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        mockMvc.perform(get("/api/v1/users?page=0&size=5")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(5)))
            .andExpect(jsonPath("$.totalElements", is(17)))
            .andExpect(jsonPath("$.totalPages", is(4)))
            .andExpect(jsonPath("$.currentPage", is(0)))
            .andExpect(jsonPath("$.pageSize", is(5)));

        mockMvc.perform(get("/api/v1/users?page=1&size=5")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(5)))
            .andExpect(jsonPath("$.currentPage", is(1)));
    }

    @Test
    void getUsers_ShouldSupportSorting_WhenAuthenticatedAsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/users?sort=email,desc")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].email", is("user@example.com")))
            .andExpect(jsonPath("$.content[1].email", is("admin@example.com")));

        mockMvc.perform(get("/api/v1/users?sort=email,asc")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].email", is("admin@example.com")))
            .andExpect(jsonPath("$.content[1].email", is("user@example.com")));
    }

    @Test
    void getUsers_ShouldFilterByEmail_WhenEmailParameterProvided() throws Exception {
        mockMvc.perform(get("/api/v1/users?email=admin@example.com")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].email", is("admin@example.com")));

        mockMvc.perform(get("/api/v1/users?email=nonexistent@example.com")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void getUsers_ShouldReturnForbidden_WhenAuthenticatedAsUser() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void getUsers_ShouldReturnUnauthorized_WhenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserById_ShouldReturnUser_WhenAuthenticatedAsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + regularUser.getId())
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id", is(regularUser.getId().intValue())))
            .andExpect(jsonPath("$.email", is("user@example.com")))
            .andExpect(jsonPath("$.firstName", is("Regular")))
            .andExpect(jsonPath("$.lastName", is("User")))
            .andExpect(jsonPath("$.roleName", is("USER")))
            .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    void getUserById_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/users/99999")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status", is(404)))
            .andExpect(jsonPath("$.message", containsString("User not found")));
    }

    @Test
    void getUserById_ShouldReturnForbidden_WhenAuthenticatedAsUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + adminUser.getId())
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void getCurrentUser_ShouldReturnCurrentUserProfile_WhenAuthenticatedAsUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id", is(regularUser.getId().intValue())))
            .andExpect(jsonPath("$.email", is("user@example.com")))
            .andExpect(jsonPath("$.firstName", is("Regular")))
            .andExpect(jsonPath("$.lastName", is("User")))
            .andExpect(jsonPath("$.roleName", is("USER")));
    }

    @Test
    void getCurrentUser_ShouldReturnCurrentUserProfile_WhenAuthenticatedAsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id", is(adminUser.getId().intValue())))
            .andExpect(jsonPath("$.email", is("admin@example.com")))
            .andExpect(jsonPath("$.firstName", is("Admin")))
            .andExpect(jsonPath("$.lastName", is("User")))
            .andExpect(jsonPath("$.roleName", is("ADMIN")));
    }

    @Test
    void getCurrentUser_ShouldReturnUnauthorized_WhenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createUser_ShouldReturnCreatedUser_WhenAuthenticatedAsAdmin() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
            "newuser@example.com",
            "password123",
            "New",
            "User",
            userRole.getId()
        );

        mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.email", is("newuser@example.com")))
            .andExpect(jsonPath("$.firstName", is("New")))
            .andExpect(jsonPath("$.lastName", is("User")))
            .andExpect(jsonPath("$.roleName", is("USER")));

        assertThat(userRepository.findByEmail("newuser@example.com")).isPresent();
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenEmailIsInvalid() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
            "invalid-email",
            "password123",
            "New",
            "User",
            userRole.getId()
        );

        mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.message", containsString("Validation failed")));
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenPasswordIsTooShort() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
            "newuser@example.com",
            "short",
            "New",
            "User",
            userRole.getId()
        );

        mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.message", containsString("Validation failed")));
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenRequiredFieldsAreMissing() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
            null,
            null,
            null,
            null,
            null
        );

        mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.message", containsString("Validation failed")))
            .andExpect(jsonPath("$.errors", notNullValue()));
    }

    @Test
    void createUser_ShouldReturnForbidden_WhenAuthenticatedAsUser() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
            "newuser@example.com",
            "password123",
            "New",
            "User",
            userRole.getId()
        );

        mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_ShouldReturnUpdatedUser_WhenAuthenticatedAsAdmin() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest(
            "updated@example.com",
            "Updated",
            "Name",
            userRole.getId()
        );

        mockMvc.perform(put("/api/v1/users/" + regularUser.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id", is(regularUser.getId().intValue())))
            .andExpect(jsonPath("$.email", is("updated@example.com")))
            .andExpect(jsonPath("$.firstName", is("Updated")))
            .andExpect(jsonPath("$.lastName", is("Name")));

        User updatedUser = userRepository.findById(regularUser.getId()).orElseThrow();
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedUser.getFirstName()).isEqualTo("Updated");
        assertThat(updatedUser.getLastName()).isEqualTo("Name");
    }

    @Test
    void updateUser_ShouldReturnConflict_WhenEmailAlreadyExists() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest(
            "admin@example.com",
            "Updated",
            "Name",
            userRole.getId()
        );

        mockMvc.perform(put("/api/v1/users/" + regularUser.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status", is(409)))
            .andExpect(jsonPath("$.message", containsString("Email already in use")));
    }

    @Test
    void updateUser_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest(
            "updated@example.com",
            "Updated",
            "Name",
            userRole.getId()
        );

        mockMvc.perform(put("/api/v1/users/99999")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status", is(404)))
            .andExpect(jsonPath("$.message", containsString("User not found")));
    }

    @Test
    void updateUser_ShouldReturnForbidden_WhenAuthenticatedAsUser() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest(
            "updated@example.com",
            "Updated",
            "Name",
            userRole.getId()
        );

        mockMvc.perform(put("/api/v1/users/" + regularUser.getId())
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_ShouldReturnBadRequest_WhenEmailIsInvalid() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest(
            "invalid-email",
            "Updated",
            "Name",
            userRole.getId()
        );

        mockMvc.perform(put("/api/v1/users/" + regularUser.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.message", containsString("Validation failed")));
    }

    @Test
    void deleteUser_ShouldReturnNoContent_WhenAuthenticatedAsAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/users/" + regularUser.getId())
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());

        assertThat(userRepository.findById(regularUser.getId())).isEmpty();
    }

    @Test
    void deleteUser_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/v1/users/99999")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status", is(404)))
            .andExpect(jsonPath("$.message", containsString("User not found")));
    }

    @Test
    void deleteUser_ShouldReturnForbidden_WhenAuthenticatedAsUser() throws Exception {
        mockMvc.perform(delete("/api/v1/users/" + regularUser.getId())
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
    }
}
