package app.userservice.service;

import app.userservice.dto.AuthResponse;
import app.userservice.dto.LoginRequest;
import app.userservice.dto.RegisterRequest;
import app.userservice.exception.DuplicateEmailException;
import app.userservice.model.Role;
import app.userservice.model.User;
import app.userservice.repository.RoleRepository;
import app.userservice.repository.UserRepository;
import app.userservice.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_ShouldCreateUserWithUserRole_WhenValidRequest() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "John", "Doe");
        Role userRole = new Role(2L, "USER");
        User savedUser = new User(1L, "test@example.com", "hashedPassword", "John", "Doe", userRole, null);
        String expectedToken = "jwt-token";

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateToken(savedUser)).thenReturn(expectedToken);

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo(expectedToken);
        verify(userRepository).existsByEmail(request.getEmail());
        verify(roleRepository).findByName("USER");
        verify(passwordEncoder).encode(request.getPassword());
        verify(userRepository).save(any(User.class));
        verify(jwtTokenProvider).generateToken(savedUser);
    }

    @Test
    void register_ShouldThrowDuplicateEmailException_WhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "John", "Doe");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(DuplicateEmailException.class)
            .hasMessageContaining("Email already in use");

        verify(userRepository).existsByEmail(request.getEmail());
        verify(roleRepository, never()).findByName(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_ShouldThrowIllegalStateException_WhenUserRoleNotFound() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "John", "Doe");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("USER role not found");

        verify(userRepository).existsByEmail(request.getEmail());
        verify(roleRepository).findByName("USER");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_ShouldReturnToken_WhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        Role userRole = new Role(2L, "USER");
        User user = new User(1L, "test@example.com", "hashedPassword", "John", "Doe", userRole, null);
        String expectedToken = "jwt-token";

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtTokenProvider.generateToken(user)).thenReturn(expectedToken);

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo(expectedToken);
        verify(userRepository).findByEmail(request.getEmail());
        verify(passwordEncoder).matches(request.getPassword(), user.getPassword());
        verify(jwtTokenProvider).generateToken(user);
    }

    @Test
    void login_ShouldThrowBadCredentialsException_WhenUserNotFound() {
        LoginRequest request = new LoginRequest("nonexistent@example.com", "password123");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("Invalid email or password");

        verify(userRepository).findByEmail(request.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtTokenProvider, never()).generateToken(any(User.class));
    }

    @Test
    void login_ShouldThrowBadCredentialsException_WhenPasswordIsIncorrect() {
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");
        Role userRole = new Role(2L, "USER");
        User user = new User(1L, "test@example.com", "hashedPassword", "John", "Doe", userRole, null);

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("Invalid email or password");

        verify(userRepository).findByEmail(request.getEmail());
        verify(passwordEncoder).matches(request.getPassword(), user.getPassword());
        verify(jwtTokenProvider, never()).generateToken(any(User.class));
    }
}
