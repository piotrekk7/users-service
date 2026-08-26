package app.userservice.service;

import app.userservice.exception.EntityNotFoundException;
import app.userservice.model.Role;
import app.userservice.model.User;
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

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId(2L);
        userRole.setName("USER");

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
}
