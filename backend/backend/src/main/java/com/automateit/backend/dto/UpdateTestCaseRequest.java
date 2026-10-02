package com.automateit.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateTestCaseRequest(
        @NotBlank(message = "Name is required") @Size(max = 150) String name,
        @NotBlank(message = "Method is required")
        @Pattern(regexp = "(?i)GET|POST|PUT|PATCH|DELETE", message = "Method must be GET, POST, PUT, PATCH or DELETE") String method,
        @NotBlank(message = "Endpoint is required") @Size(max = 500)
        @Pattern(regexp = "^/.*", message = "Endpoint must start with /") String endpoint,
        @Size(max = 2000) String description
) {}
