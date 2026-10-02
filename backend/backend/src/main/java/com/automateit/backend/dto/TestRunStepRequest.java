package com.automateit.backend.dto;

import com.automateit.backend.enums.ExecutionItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TestRunStepRequest(
        @NotBlank(message = "Step description is required") @Size(max = 500) String description,
        @Size(max = 2000) String expectedResult,
        @Size(max = 2000) String actualResult,
        @NotNull(message = "Step status is required") ExecutionItemStatus status
) {}
