package com.automateit.backend.dto;

import com.automateit.backend.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record CreateUserRequest(
        @NotBlank(message = "Username is required")
        @Size(max = 100, message = "Username must contain at most 100 characters")
        @Pattern(regexp = "[A-Za-z0-9._-]+", message = "Username contains invalid characters") String username,
        @NotBlank(message = "Display name is required")
        @Size(max = 150, message = "Display name must contain at most 150 characters") String displayName,
        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 72, message = "Password must contain between 12 and 72 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "Password must include uppercase, lowercase and a number") String password,
        @NotNull(message = "Role is required") UserRole role,
        @NotNull(message = "Projects are required") Set<UUID> projectIds
) {}
