package com.automateit.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetUserPasswordRequest(
        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 72, message = "Password must contain between 12 and 72 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Password must include uppercase, lowercase and a number") String password
) {}
