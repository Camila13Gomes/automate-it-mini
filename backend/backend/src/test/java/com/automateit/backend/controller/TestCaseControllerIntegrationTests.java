package com.automateit.backend.controller;

import com.automateit.backend.repository.TestCaseRepository;
import com.automateit.backend.repository.ProjectRepository;
import com.automateit.backend.repository.TestRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class TestCaseControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TestRunRepository testRunRepository;

    private UUID projectId;

    @BeforeEach
    void cleanDatabase() {
        testRunRepository.deleteAll();
        testCaseRepository.deleteAll();
        projectId = projectRepository.findBySlug("default").orElseThrow().getId();
    }

    @Test
    void createsAndNormalizesTestCase() throws Exception {
        mockMvc.perform(post("/api/test-cases")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "%s",
                                  "name": "  List test runs  ",
                                  "method": "get",
                                  "endpoint": "/api/test-runs",
                                  "description": "  Verifies the test run list  "
                                }
                                """.formatted(projectId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("List test runs"))
                .andExpect(jsonPath("$.method").value("GET"))
                .andExpect(jsonPath("$.endpoint").value("/api/test-runs"))
                .andExpect(jsonPath("$.description").value("Verifies the test run list"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void rejectsInvalidMethodAndEndpoint() throws Exception {
        mockMvc.perform(post("/api/test-cases")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "%s",
                                  "name": "Invalid test case",
                                  "method": "CONNECT",
                                  "endpoint": "api/test-runs"
                                }
                                """.formatted(projectId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.method")
                        .value("Method must be GET, POST, PUT, PATCH or DELETE"))
                .andExpect(jsonPath("$.validationErrors.endpoint")
                        .value("Endpoint must start with /"));
    }

    @Test
    void returnsNotFoundForUnknownId() throws Exception {
        UUID unknownId = UUID.randomUUID();

        mockMvc.perform(get("/api/test-cases/{id}", unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Test case not found with id: " + unknownId));
    }

    @Test
    void returnsBadRequestForMalformedId() throws Exception {
        mockMvc.perform(get("/api/test-cases/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter: id"));
    }

    @Test
    void listsTestCasesUsingPagination() throws Exception {
        mockMvc.perform(get("/api/test-cases")
                        .param("projectId", projectId.toString())
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.number").value(0));
    }
}
