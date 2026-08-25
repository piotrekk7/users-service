package app.userservice.service;

import app.userservice.dto.CreateUserRequest;
import app.userservice.exception.DuplicateEmailException;
import app.userservice.model.Role;
import app.userservice.model.User;
import app.userservice.repository.RoleRepository;
import app.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public Page<User> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
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
