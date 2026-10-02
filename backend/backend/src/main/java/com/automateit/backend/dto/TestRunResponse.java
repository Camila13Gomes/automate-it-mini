package com.automateit.backend.dto;

import com.automateit.backend.enums.EnvironmentType;
import com.automateit.backend.enums.ExecutionType;
import com.automateit.backend.enums.TestRunStatus;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

public class TestRunResponse {

    private UUID id;
    private UUID projectId;
    private UUID testCaseId;
    private String testCaseName;
    private String name;
    private String projectName;
    private EnvironmentType environment;
    private ExecutionType executionType;
    private TestRunStatus status;
    private String createdBy;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String responseBody;
    private String evidence;
    private String logs;
    private Long durationMs;
    private List<TestRunStepResponse> steps;
    private List<TestAssertionResponse> assertions;

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() { return projectId; }
    public UUID getTestCaseId() { return testCaseId; }
    public String getTestCaseName() { return testCaseName; }

    public String getName() {
        return name;
    }

    public String getProjectName() {
        return projectName;
    }

    public EnvironmentType getEnvironment() {
        return environment;
    }

    public ExecutionType getExecutionType() {
        return executionType;
    }

    public TestRunStatus getStatus() {
        return status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }
    public String getResponseBody() { return responseBody; }
    public String getEvidence() { return evidence; }
    public String getLogs() { return logs; }
    public Long getDurationMs() { return durationMs; }
    public List<TestRunStepResponse> getSteps() { return steps; }
    public List<TestAssertionResponse> getAssertions() { return assertions; }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public void setTestCaseId(UUID testCaseId) { this.testCaseId = testCaseId; }
    public void setTestCaseName(String testCaseName) { this.testCaseName = testCaseName; }

    public void setName(String name) {
        this.name = name;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setEnvironment(EnvironmentType environment) {
        this.environment = environment;
    }

    public void setExecutionType(ExecutionType executionType) {
        this.executionType = executionType;
    }

    public void setStatus(TestRunStatus status) {
        this.status = status;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public void setLogs(String logs) { this.logs = logs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public void setSteps(List<TestRunStepResponse> steps) { this.steps = steps; }
    public void setAssertions(List<TestAssertionResponse> assertions) { this.assertions = assertions; }
}
