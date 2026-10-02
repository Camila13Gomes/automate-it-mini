package com.automateit.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CreateTestCaseRequest {

    @NotNull(message = "Project is required")
    private UUID projectId;

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must contain at most 150 characters")
    private String name;

    @NotBlank(message = "Method is required")
    @Pattern(
            regexp = "(?i)GET|POST|PUT|PATCH|DELETE",
            message = "Method must be GET, POST, PUT, PATCH or DELETE"
    )
    private String method;

    @NotBlank(message = "Endpoint is required")
    @Size(max = 500, message = "Endpoint must contain at most 500 characters")
    @Pattern(regexp = "^/.*", message = "Endpoint must start with /")
    private String endpoint;

    @Size(max = 2000, message = "Description must contain at most 2000 characters")
    private String description;

    public String getName() {
        return name;
    }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public String getMethod() {
        return method;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getDescription() {
        return description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
