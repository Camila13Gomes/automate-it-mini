package com.automateit.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must contain at most 150 characters") String name,
        @NotBlank(message = "Slug is required")
        @Size(max = 100, message = "Slug must contain at most 100 characters")
        @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "Slug must use lowercase letters, numbers and hyphens") String slug
) {}
