package app.userservice.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
    int status,
    String message,
    List<String> errors,
    LocalDateTime timestamp
) {
    public ErrorResponse(int status, String message, List<String> errors) {
        this(status, message, errors, LocalDateTime.now());
    }
}
