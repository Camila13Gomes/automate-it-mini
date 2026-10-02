package com.automateit.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TestAssertionRequest(
        @NotBlank(message = "Assertion name is required") @Size(max = 300) String name,
        @Size(max = 2000) String expectedValue,
        @Size(max = 2000) String actualValue,
        boolean passed
) {}
