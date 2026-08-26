package app.userservice.service;

import app.userservice.dto.CreateUserRequest;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

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

    @Transactional
    public User createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already in use: " + request.getEmail());
        }

        Role role = roleRepository.findById(request.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + request.getRoleId()));

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(role);

        return userRepository.save(user);
    }
}
