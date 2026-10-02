package com.automateit.backend.dto;

import com.automateit.backend.enums.UserRole;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String displayName,
        UserRole role,
        boolean enabled,
        List<ProjectResponse> projects,
        LocalDateTime createdAt
) {}
