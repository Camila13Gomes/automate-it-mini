package com.automateit.backend.controller;

import com.automateit.backend.entity.Project;
import com.automateit.backend.entity.UserAccount;
import com.automateit.backend.enums.UserRole;
import com.automateit.backend.repository.ProjectRepository;
import com.automateit.backend.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired UserAccountRepository users;
    @Autowired ProjectRepository projects;
    @Autowired PasswordEncoder passwordEncoder;

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
        users.save(viewer);
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
                        .content("{\"username\":\"admin\",\"password\":\"TestAdmin9876\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn().getRequest().getSession(false);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
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
    void preventsNonAdminUserManagement() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }
}
