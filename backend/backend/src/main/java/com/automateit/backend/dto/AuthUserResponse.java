package com.automateit.backend.dto;

import com.automateit.backend.enums.UserRole;

import java.util.List;
import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String username,
        String displayName,
        UserRole role,
        List<ProjectResponse> projects
) {}
