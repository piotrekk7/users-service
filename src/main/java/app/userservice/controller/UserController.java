package app.userservice.controller;

import app.userservice.dto.PagedResponse;
import app.userservice.dto.UserResponseDto;
import app.userservice.model.User;
import app.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for managing users")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get paginated list of users", description = "Returns a paginated list of all users")
    public ResponseEntity<PagedResponse<UserResponseDto>> getUsers(
            @PageableDefault(size = 10, sort = "email", direction = Sort.Direction.ASC)
            Pageable pageable) {
        Page<User> usersPage = userService.getUsers(pageable);

        PagedResponse<UserResponseDto> response = new PagedResponse<>(
            usersPage.getContent().stream()
                .map(this::mapToUserResponseDto)
                .toList(),
            usersPage.getTotalElements(),
            usersPage.getTotalPages(),
            usersPage.getNumber(),
            usersPage.getSize()
        );

        return ResponseEntity.ok(response);
    }

    private UserResponseDto mapToUserResponseDto(User user) {
        return new UserResponseDto(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole().getName(),
            user.getCreatedAt()
        );
    }
}
