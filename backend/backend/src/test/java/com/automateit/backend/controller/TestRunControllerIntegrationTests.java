package com.automateit.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.automateit.backend.repository.TestRunRepository;
import com.automateit.backend.repository.ProjectRepository;
import com.automateit.backend.repository.TestCaseRepository;
import com.automateit.backend.entity.TestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class TestRunControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestRunRepository testRunRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    private UUID projectId;
    private UUID testCaseId;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @BeforeEach
    void cleanDatabase() {
        testRunRepository.deleteAll();
        var project = projectRepository.findBySlug("default").orElseThrow();
        projectId = project.getId();
        TestCase testCase = new TestCase();
        testCase.setProject(project);
        testCase.setName("Login API");
        testCase.setMethod("POST");
        testCase.setEndpoint("/api/login");
        testCaseId = testCaseRepository.save(testCase).getId();
    }

    @Test
    void createsTestRunWithGeneratedIdAndInitialState() throws Exception {
        mockMvc.perform(post("/api/test-runs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.testCaseId").value(testCaseId.toString()))
                .andExpect(jsonPath("$.testCaseName").value("Login API"))
                .andExpect(jsonPath("$.name").value("Smoke test"));
    }

    @Test
    void returnsStructuredValidationErrors() throws Exception {
        mockMvc.perform(post("/api/test-runs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "%s",
                                  "testCaseId": "%s",
                                  "name": " ",
                                  "environment": "QA",
                                  "executionType": "SMOKE",
                                  "createdBy": "developer"
                                }
                                """.formatted(projectId, testCaseId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.validationErrors.name").value("Name is required"));
    }

    @Test
    void rejectsInvalidStatusTransition() throws Exception {
        String response = mockMvc.perform(post("/api/test-runs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(patch("/api/test-runs/{id}/status", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PASSED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Invalid test run status transition from PENDING to PASSED"));
    }

    @Test
    void storesExecutionDetailsAndCalculatesDuration() throws Exception {
        String response = mockMvc.perform(post("/api/test-runs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(patch("/api/test-runs/{id}/details", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "responseBody": "{\\\"token\\\":\\\"hidden\\\"}",
                                  "evidence": "screenshot://login.png",
                                  "logs": "request completed with HTTP 200",
                                  "steps": [{
                                    "description": "Send valid credentials",
                                    "expectedResult": "HTTP 200",
                                    "actualResult": "HTTP 200",
                                    "status": "PASSED"
                                  }],
                                  "assertions": [{
                                    "name": "Status code",
                                    "expectedValue": "200",
                                    "actualValue": "200",
                                    "passed": true
                                  }]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseBody").value("{\"token\":\"hidden\"}"))
                .andExpect(jsonPath("$.steps[0].orderIndex").value(1))
                .andExpect(jsonPath("$.steps[0].status").value("PASSED"))
                .andExpect(jsonPath("$.assertions[0].passed").value(true));

        mockMvc.perform(patch("/api/test-runs/{id}/status", id)
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RUNNING\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/test-runs/{id}/status", id)
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PASSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finished").doesNotExist())
                .andExpect(jsonPath("$.durationMs").isNumber())
                .andExpect(jsonPath("$.finishedAt").isNotEmpty());
    }

    @Test
    void filtersRunsBySearchAndTestCaseWithPagination() throws Exception {
        mockMvc.perform(post("/api/test-runs")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/test-runs")
                        .param("projectId", projectId.toString())
                        .param("testCaseId", testCaseId.toString())
                        .param("q", "smoke")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page.size").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    private String validCreateRequest() {
        return """
                {
                  "projectId": "%s",
                  "testCaseId": "%s",
                  "name": "  Smoke test  ",
                  "environment": "QA",
                  "executionType": "SMOKE",
                  "notes": "Initial execution"
                }
                """.formatted(projectId, testCaseId);
    }
}
