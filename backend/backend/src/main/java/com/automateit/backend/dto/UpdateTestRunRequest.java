package com.automateit.backend.dto;

import com.automateit.backend.enums.EnvironmentType;
import com.automateit.backend.enums.ExecutionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateTestRunRequest(
        @NotBlank(message = "Name is required") @Size(max = 150) String name,
        @NotNull(message = "Test case is required") UUID testCaseId,
        @NotNull(message = "Environment is required") EnvironmentType environment,
        @NotNull(message = "Execution type is required") ExecutionType executionType,
        @Size(max = 2000) String notes
) {}
