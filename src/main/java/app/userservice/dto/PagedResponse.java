package app.userservice.dto;

import java.util.List;

public record PagedResponse<T>(
    List<T> content,
    long totalElements,
    int totalPages,
    int currentPage,
    int pageSize
) {
}
