package com.automateit.backend.controller;

import com.automateit.backend.entity.Project;
import com.automateit.backend.entity.UserAccount;
import com.automateit.backend.enums.UserRole;
import com.automateit.backend.repository.ProjectRepository;
import com.automateit.backend.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired UserAccountRepository users;
    @Autowired ProjectRepository projects;
    @Autowired com.automateit.backend.repository.TestCaseRepository testCases;
    @Autowired com.automateit.backend.repository.TestRunRepository testRuns;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired com.automateit.backend.config.BootstrapData bootstrapData;

    private UUID defaultProjectId;
    private UUID privateProjectId;

    @BeforeEach
    void prepareUsersAndProjects() {
        Project defaultProject = projects.findBySlug("default").orElseThrow();
        Project privateProject = projects.findBySlug("private").orElseGet(() -> {
            Project project = new Project();
            project.setName("Private Project");
            project.setSlug("private");
            return projects.save(project);
        });
        UserAccount viewer = users.findByUsernameIgnoreCase("viewer").orElseGet(UserAccount::new);
        viewer.setUsername("viewer");
        viewer.setDisplayName("Read Only User");
        viewer.setPasswordHash(passwordEncoder.encode("viewer123"));
        viewer.setRole(UserRole.VIEWER);
        viewer.setProjects(Set.of(defaultProject));
        viewer.setEnabled(true);
        users.save(viewer);
        prepareUser("manager", "Manager User", UserRole.MANAGER, defaultProject);
        prepareUser("tester", "Tester User", UserRole.TESTER, defaultProject);
        UserAccount lockUser = users.findByUsernameIgnoreCase("locktest").orElseGet(UserAccount::new);
        lockUser.setUsername("locktest");
        lockUser.setDisplayName("Lock Test User");
        lockUser.setPasswordHash(passwordEncoder.encode("LockTest1234"));
        lockUser.setRole(UserRole.VIEWER);
        lockUser.setProjects(Set.of(defaultProject));
        lockUser.setFailedLoginAttempts(0);
        lockUser.setLockedUntil(null);
        users.save(lockUser);
        defaultProjectId = defaultProject.getId();
        privateProjectId = privateProject.getId();
    }

    private void prepareUser(String username, String displayName, UserRole role, Project project) {
        UserAccount account = users.findByUsernameIgnoreCase(username).orElseGet(UserAccount::new);
        account.setUsername(username);
        account.setDisplayName(displayName);
        account.setPasswordHash(passwordEncoder.encode("ValidPassword123"));
        account.setRole(role);
        account.setProjects(Set.of(project));
        account.setEnabled(true);
        account.setFailedLoginAttempts(0);
        account.setLockedUntil(null);
        users.save(account);
    }


    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void preventsDirectAccessToExistingPrivateTestRun() throws Exception {
        Project privateProject = projects.findBySlug("private").orElseThrow();

        var testCase = new com.automateit.backend.entity.TestCase();
        testCase.setProject(privateProject);
        testCase.setName("Private API Test");
        testCase.setMethod("GET");
        testCase.setEndpoint("/api/private");
        testCase = testCases.save(testCase);

        var run = new com.automateit.backend.entity.TestRun();
        run.setProject(privateProject);
        run.setTestCase(testCase);
        run.setName("Private execution");
        run.setProjectName(privateProject.getName());
        run.setEnvironment(com.automateit.backend.enums.EnvironmentType.QA);
        run.setExecutionType(com.automateit.backend.enums.ExecutionType.SMOKE);
        run.setStatus(com.automateit.backend.enums.TestRunStatus.PENDING);
        run.setCreatedBy("admin");
        run = testRuns.save(run);

        mockMvc.perform(get("/api/test-runs/{id}", run.getId()))
                .andExpect(status().isForbidden());
    }


    @Test
    void rejectsUnauthenticatedApiRequests() throws Exception {
        mockMvc.perform(get("/api/test-cases").param("projectId", defaultProjectId.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void authenticatesWithPersistedUserAndServerSession() throws Exception {
        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"  ADMIN  \",\"password\":\"TestAdmin9876\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn().getRequest().getSession(false);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void exposesCsrfTokenForTheFrontend() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").isNotEmpty());
    }

    @Test
    void bootstrapDoesNotOverwritePasswordChangedInTheApplication() throws Exception {
        UserAccount admin = users.findByUsernameIgnoreCase("admin").orElseThrow();
        String originalHash = admin.getPasswordHash();
        try {
            admin.setPasswordHash(passwordEncoder.encode("ChangedAdmin9876"));
            users.saveAndFlush(admin);

            bootstrapData.run(new DefaultApplicationArguments(new String[0]));

            UserAccount persisted = users.findByUsernameIgnoreCase("admin").orElseThrow();
            org.junit.jupiter.api.Assertions.assertTrue(
                    passwordEncoder.matches("ChangedAdmin9876", persisted.getPasswordHash()));
        } finally {
            UserAccount persisted = users.findByUsernameIgnoreCase("admin").orElseThrow();
            persisted.setPasswordHash(originalHash);
            users.saveAndFlush(persisted);
        }
    }


    @Test
    void logoutInvalidatesAuthenticatedSession() throws Exception {
        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"viewer","password":"viewer123"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isOk());

        org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void storesOnlyPasswordHashAndLocksRepeatedFailures() throws Exception {
        UserAccount persisted = users.findByUsernameIgnoreCase("locktest").orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotEquals("LockTest1234", persisted.getPasswordHash());
        org.junit.jupiter.api.Assertions.assertTrue(persisted.getPasswordHash().startsWith("$2"));

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/auth/login").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"locktest\",\"password\":\"WrongPassword1\"}"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"locktest\",\"password\":\"LockTest1234\"}"))
                .andExpect(status().isUnauthorized());
        org.junit.jupiter.api.Assertions.assertNotNull(
                users.findByUsernameIgnoreCase("locktest").orElseThrow().getLockedUntil());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void viewerCanReadAssignedProjectButCannotWrite() throws Exception {
        mockMvc.perform(get("/api/test-cases").param("projectId", defaultProjectId.toString()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/test-cases").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","name":"Forbidden","method":"GET","endpoint":"/api/demo"}
                                """.formatted(defaultProjectId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void preventsCrossProjectAccess() throws Exception {
        mockMvc.perform(get("/api/test-runs").param("projectId", privateProjectId.toString()))
                .andExpect(status().isForbidden());
    }


    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void preventsDirectAccessToPrivateTestRun() throws Exception {
        mockMvc.perform(get("/api/test-runs/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void preventsNonAdminUserManagement() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "tester", roles = "TESTER")
    void testerExecutesAndRecordsResultsButCannotManageDefinitionsOrDeleteRuns() throws Exception {
        Project project = projects.findById(defaultProjectId).orElseThrow();
        var testCase = new com.automateit.backend.entity.TestCase();
        testCase.setProject(project);
        testCase.setName("Tester permission case " + UUID.randomUUID());
        testCase.setMethod("GET");
        testCase.setEndpoint("/api/permission-check");
        testCase = testCases.save(testCase);

        mockMvc.perform(post("/api/test-cases").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","name":"Forbidden definition","method":"GET","endpoint":"/api/forbidden"}
                                """.formatted(defaultProjectId)))
                .andExpect(status().isForbidden());

        String runName = "Tester execution " + UUID.randomUUID();
        mockMvc.perform(post("/api/test-runs").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","testCaseId":"%s","name":"%s","environment":"QA","executionType":"SMOKE"}
                                """.formatted(defaultProjectId, testCase.getId(), runName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        var run = testRuns.findAll().stream().filter(item -> item.getName().equals(runName)).findFirst().orElseThrow();
        mockMvc.perform(patch("/api/test-runs/{id}/details", run.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"responseBody":"ok","evidence":"ticket-123","logs":"completed","steps":[],"assertions":[]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseBody").value("ok"));

        mockMvc.perform(patch("/api/test-runs/{id}/status", run.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"status\":\"RUNNING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));

        mockMvc.perform(put("/api/test-runs/{id}", run.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Forbidden edit","testCaseId":"%s","environment":"QA","executionType":"SMOKE"}
                                """.formatted(testCase.getId())))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/test-runs/{id}", run.getId()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void managerCanCreateEditAndDeleteTestDefinitions() throws Exception {
        String caseName = "Manager case " + UUID.randomUUID();
        mockMvc.perform(post("/api/test-cases").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","name":"%s","method":"GET","endpoint":"/api/manager"}
                                """.formatted(defaultProjectId, caseName)))
                .andExpect(status().isCreated());

        var testCase = testCases.findAll().stream().filter(item -> item.getName().equals(caseName)).findFirst().orElseThrow();
        mockMvc.perform(put("/api/test-cases/{id}", testCase.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Manager updated case","method":"POST","endpoint":"/api/manager/updated","description":"updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("POST"));
        mockMvc.perform(delete("/api/test-cases/{id}", testCase.getId()).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void administratorCanManageTheCompleteUserLifecycle() throws Exception {
        String username = "managed-" + UUID.randomUUID();
        mockMvc.perform(post("/api/users").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","displayName":"Managed User","password":"InitialPass123","role":"TESTER","projectIds":["%s"]}
                                """.formatted(username, defaultProjectId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enabled").value(true));

        UserAccount managed = users.findByUsernameIgnoreCase(username).orElseThrow();
        mockMvc.perform(put("/api/users/{id}", managed.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","displayName":"Updated User","role":"MANAGER","projectIds":["%s"]}
                                """.formatted(username, defaultProjectId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"));

        managed = users.findById(managed.getId()).orElseThrow();
        managed.setFailedLoginAttempts(5);
        managed.setLockedUntil(java.time.LocalDateTime.now().plusMinutes(15));
        users.saveAndFlush(managed);
        mockMvc.perform(put("/api/users/{id}/password", managed.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"ReplacementPass456\"}"))
                .andExpect(status().isNoContent());
        UserAccount passwordUpdated = users.findById(managed.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("ReplacementPass456", passwordUpdated.getPasswordHash()));
        org.junit.jupiter.api.Assertions.assertEquals(0, passwordUpdated.getFailedLoginAttempts());
        org.junit.jupiter.api.Assertions.assertNull(passwordUpdated.getLockedUntil());

        mockMvc.perform(patch("/api/users/{id}/enabled", managed.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
        mockMvc.perform(patch("/api/users/{id}/enabled", managed.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        mockMvc.perform(delete("/api/users/{id}", managed.getId()).with(csrf()))
                .andExpect(status().isNoContent());
        org.junit.jupiter.api.Assertions.assertFalse(users.existsById(managed.getId()));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void administratorCannotDisableOrDeleteOwnAccount() throws Exception {
        UUID adminId = users.findByUsernameIgnoreCase("admin").orElseThrow().getId();
        mockMvc.perform(patch("/api/users/{id}/enabled", adminId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/users/{id}", adminId).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "tester", roles = "TESTER")
    void disabledUserCannotContinueUsingAnExistingSession() throws Exception {
        UserAccount tester = users.findByUsernameIgnoreCase("tester").orElseThrow();
        tester.setEnabled(false);
        users.saveAndFlush(tester);

        mockMvc.perform(get("/api/test-cases").param("projectId", defaultProjectId.toString()))
                .andExpect(status().isForbidden());
    }
}
