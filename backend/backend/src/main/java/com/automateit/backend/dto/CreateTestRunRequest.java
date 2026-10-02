package com.automateit.backend.dto;

import com.automateit.backend.enums.EnvironmentType;
import com.automateit.backend.enums.ExecutionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class CreateTestRunRequest {

    @NotNull(message = "Project is required")
    private UUID projectId;

    @NotNull(message = "Test case is required")
    private UUID testCaseId;

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must contain at most 150 characters")
    private String name;

    @NotNull(message = "Environment is required")
    private EnvironmentType environment;

    @NotNull(message = "Execution type is required")
    private ExecutionType executionType;

    @Size(max = 2000, message = "Notes must contain at most 2000 characters")
    private String notes;

    public String getName() {
        return name;
    }

    public UUID getProjectId() { return projectId; }
    public UUID getTestCaseId() { return testCaseId; }

    public EnvironmentType getEnvironment() {
        return environment;
    }

    public ExecutionType getExecutionType() {
        return executionType;
    }

    public String getNotes() {
        return notes;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public void setTestCaseId(UUID testCaseId) { this.testCaseId = testCaseId; }

    public void setEnvironment(EnvironmentType environment) {
        this.environment = environment;
    }

    public void setExecutionType(ExecutionType executionType) {
        this.executionType = executionType;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
