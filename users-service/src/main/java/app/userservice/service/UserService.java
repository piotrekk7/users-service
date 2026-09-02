package app.userservice.service;

import app.userservice.audit.AuditAction;
import app.userservice.audit.AuditEventPublisher;
import app.userservice.audit.RequestContextExtractor;
import app.userservice.dto.AuditEvent;
import app.userservice.dto.CreateUserRequest;
import app.userservice.dto.UpdateUserRequest;
import app.userservice.exception.DuplicateEmailException;
import app.userservice.exception.EntityNotFoundException;
import app.userservice.model.Role;
import app.userservice.model.User;
import app.userservice.repository.RoleRepository;
import app.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventPublisher auditEventPublisher;
    private final RequestContextExtractor requestContextExtractor;

    public Page<User> getUsers(Pageable pageable, String email) {
        if (email != null && !email.isBlank()) {
            return userRepository.findByEmail(email)
                .map(user -> new PageImpl<>(List.of(user), pageable, 1))
                .orElse(new PageImpl<>(List.of(), pageable, 0));
        }
        return userRepository.findAll(pageable);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());
        return getUserById(userId);
    }

    @Transactional
    public User createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already in use: " + request.getEmail());
        }

        Role role = roleRepository.findById(request.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + request.getRoleId()));

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(role);

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        userRepository.findByEmail(request.getEmail())
            .filter(existingUser -> !existingUser.getId().equals(id))
            .ifPresent(existingUser -> {
                throw new DuplicateEmailException("Email already in use: " + request.getEmail());
            });

        Role role = roleRepository.findById(request.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + request.getRoleId()));

        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(role);

        User updatedUser = userRepository.save(user);
        publishAuditEvent(updatedUser.getId().toString(), AuditAction.USER_UPDATED);
        return updatedUser;
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        userRepository.delete(user);
        publishAuditEvent(user.getId().toString(), AuditAction.USER_DELETED);
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
