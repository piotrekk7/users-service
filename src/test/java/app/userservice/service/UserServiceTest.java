package app.userservice.service;

import app.userservice.dto.UpdateUserRequest;
import app.userservice.exception.DuplicateEmailException;
import app.userservice.exception.EntityNotFoundException;
import app.userservice.model.Role;
import app.userservice.model.User;
import app.userservice.repository.RoleRepository;
import app.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId(2L);
        userRole.setName("USER");

        adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName("ADMIN");

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPassword("hashedPassword");
        testUser.setRole(userRole);
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void getUserById_ShouldReturnUser_WhenUserExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        User result = userService.getUserById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_ShouldThrowEntityNotFoundException_WhenUserDoesNotExist() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessage("User not found with id: 999");

        verify(userRepository).findById(999L);
    }

    @Test
    void getUsers_ShouldReturnAllUsers_WhenEmailIsNull() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(pageable)).thenReturn(userPage);

        Page<User> result = userService.getUsers(pageable, null);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("test@example.com");
        verify(userRepository).findAll(pageable);
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void getUsers_ShouldReturnAllUsers_WhenEmailIsBlank() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(pageable)).thenReturn(userPage);

        Page<User> result = userService.getUsers(pageable, "   ");

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findAll(pageable);
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void getUsers_ShouldReturnFilteredUser_WhenEmailMatches() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        Page<User> result = userService.getUsers(pageable, "test@example.com");

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("test@example.com");
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getUsers_ShouldReturnEmptyPage_WhenEmailDoesNotMatch() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        Page<User> result = userService.getUsers(pageable, "nonexistent@example.com");

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void updateUser_ShouldUpdateUser_WhenValidRequest() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("updated@example.com");
        request.setFirstName("Jane");
        request.setLastName("Smith");
        request.setRoleId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("updated@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateUser(1L, request);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("updated@example.com");
        assertThat(result.getFirstName()).isEqualTo("Jane");
        assertThat(result.getLastName()).isEqualTo("Smith");
        assertThat(result.getRole().getName()).isEqualTo("ADMIN");
        verify(userRepository).findById(1L);
        verify(userRepository).findByEmail("updated@example.com");
        verify(roleRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUser_ShouldAllowSameEmail_WhenUpdatingSameUser() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("test@example.com");
        request.setFirstName("John");
        request.setLastName("Updated");
        request.setRoleId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateUser(1L, request);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getLastName()).isEqualTo("Updated");
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUser_ShouldThrowDuplicateEmailException_WhenEmailTakenByAnotherUser() {
        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setEmail("taken@example.com");

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("taken@example.com");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setRoleId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(anotherUser));

        assertThatThrownBy(() -> userService.updateUser(1L, request))
            .isInstanceOf(DuplicateEmailException.class)
            .hasMessage("Email already in use: taken@example.com");

        verify(userRepository).findById(1L);
        verify(userRepository).findByEmail("taken@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_ShouldThrowEntityNotFoundException_WhenUserDoesNotExist() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("test@example.com");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setRoleId(2L);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(999L, request))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessage("User not found with id: 999");

        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_ShouldThrowIllegalArgumentException_WhenRoleDoesNotExist() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("test@example.com");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setRoleId(999L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Role not found with id: 999");

        verify(userRepository).findById(1L);
        verify(roleRepository).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteUser_ShouldDeleteUser_WhenUserExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        userService.deleteUser(1L);

        verify(userRepository).findById(1L);
        verify(userRepository).delete(testUser);
    }

    @Test
    void deleteUser_ShouldThrowEntityNotFoundException_WhenUserDoesNotExist() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(999L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessage("User not found with id: 999");

        verify(userRepository).findById(999L);
        verify(userRepository, never()).delete(any(User.class));
    }
}
