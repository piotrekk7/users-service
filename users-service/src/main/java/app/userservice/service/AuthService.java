package app.userservice.service;

import app.userservice.audit.AuditAction;
import app.userservice.audit.AuditEventPublisher;
import app.userservice.audit.RequestContextExtractor;
import app.userservice.dto.AuditEvent;
import app.userservice.dto.AuthResponse;
import app.userservice.dto.LoginRequest;
import app.userservice.dto.RegisterRequest;
import app.userservice.event.UserRegisteredEvent;
import app.userservice.exception.DuplicateEmailException;
import app.userservice.model.Role;
import app.userservice.model.User;
import app.userservice.repository.RoleRepository;
import app.userservice.repository.UserRepository;
import app.userservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserEventPublisher userEventPublisher;
    private final AuditEventPublisher auditEventPublisher;
    private final RequestContextExtractor requestContextExtractor;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already in use: " + request.getEmail());
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("USER role not found in database"));

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(userRole);

        User savedUser = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(savedUser);

        UserRegisteredEvent event = new UserRegisteredEvent(
                savedUser.getEmail(),
                savedUser.getFirstName() + " " + savedUser.getLastName(),
                savedUser.getCreatedAt()
        );
        userEventPublisher.publishUserRegisteredEvent(event);

        publishAuditEvent(savedUser.getId().toString(), AuditAction.USER_REGISTERED);

        return new AuthResponse(token);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    publishAuditEvent("unknown", AuditAction.USER_LOGIN_FAILED);
                    return new BadCredentialsException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            publishAuditEvent(user.getId().toString(), AuditAction.USER_LOGIN_FAILED);
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(user);
        publishAuditEvent(user.getId().toString(), AuditAction.USER_LOGIN_SUCCESS);
        return new AuthResponse(token);
    }

    private void publishAuditEvent(String userId, AuditAction action) {
        AuditEvent auditEvent = AuditEvent.builder()
                .userId(userId)
                .action(action.name())
                .ipAddress(requestContextExtractor.getIpAddress())
                .userAgent(requestContextExtractor.getUserAgent())
                .requestDetails(AuditEvent.RequestDetails.builder()
                        .endpoint(requestContextExtractor.getRequestUri())
                        .method(requestContextExtractor.getRequestMethod())
                        .build())
                .build();

        auditEventPublisher.publishAuditEvent(auditEvent);
    }
}
