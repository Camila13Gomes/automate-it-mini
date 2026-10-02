package com.automateit.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateTestRunDetailsRequest(
        @Size(max = 100000, message = "Response body is too large") String responseBody,
        @Size(max = 20000, message = "Evidence is too large") String evidence,
        @Size(max = 100000, message = "Logs are too large") String logs,
        List<@Valid TestRunStepRequest> steps,
        List<@Valid TestAssertionRequest> assertions
) {}
