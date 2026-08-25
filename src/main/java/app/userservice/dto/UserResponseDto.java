package app.userservice.dto;

import java.time.LocalDateTime;

public record UserResponseDto(
    Long id,
    String email,
    String firstName,
    String lastName,
    String roleName,
    LocalDateTime createdAt
) {
}
