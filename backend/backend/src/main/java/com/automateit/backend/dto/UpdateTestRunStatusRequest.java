package com.automateit.backend.dto;

import com.automateit.backend.enums.TestRunStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateTestRunStatusRequest {

    @NotNull(message = "Status is required")
    private TestRunStatus status;

    public TestRunStatus getStatus() {
        return status;
    }

    public void setStatus(TestRunStatus status) {
        this.status = status;
    }
}
