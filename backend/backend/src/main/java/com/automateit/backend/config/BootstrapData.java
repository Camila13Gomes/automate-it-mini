package com.automateit.backend.config;

import com.automateit.backend.entity.Project;
import com.automateit.backend.entity.UserAccount;
import com.automateit.backend.enums.UserRole;
import com.automateit.backend.repository.ProjectRepository;
import com.automateit.backend.repository.TestCaseRepository;
import com.automateit.backend.repository.TestRunRepository;
import com.automateit.backend.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class BootstrapData implements ApplicationRunner {
    private final ProjectRepository projects;
    private final UserAccountRepository users;
    private final TestCaseRepository testCases;
    private final TestRunRepository testRuns;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private final String adminDisplayName;

    public BootstrapData(ProjectRepository projects, UserAccountRepository users,
                         TestCaseRepository testCases, TestRunRepository testRuns,
                         PasswordEncoder passwordEncoder,
                         @Value("${app.bootstrap.admin-username}") String adminUsername,
                         @Value("${app.bootstrap.admin-password}") String adminPassword,
                         @Value("${app.bootstrap.admin-display-name}") String adminDisplayName) {
        this.projects = projects;
        this.users = users;
        this.testCases = testCases;
        this.testRuns = testRuns;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.adminDisplayName = adminDisplayName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String normalizedAdminUsername = normalizeAdminUsername();
        String normalizedAdminDisplayName = normalizeAdminDisplayName();
        validateBootstrapPassword();

        Project defaultProject = projects.findBySlug("default").orElseGet(() -> {
            Project project = new Project();
            project.setName("Default Project");
            project.setSlug("default");
            return projects.save(project);
        });

        testCases.findAll().stream().filter(testCase -> testCase.getProject() == null).forEach(testCase -> {
            testCase.setProject(defaultProject);
            testCases.save(testCase);
        });
        testRuns.findAll().stream().filter(testRun -> testRun.getProject() == null).forEach(testRun -> {
            testRun.setProject(defaultProject);
            testRun.setProjectName(defaultProject.getName());
            testRuns.save(testRun);
        });
        testRuns.findAll().stream().filter(testRun -> testRun.getTestCase() == null).forEach(testRun -> {
            Project project = testRun.getProject();
            var legacyCase = testCases.findAll().stream()
                    .filter(item -> item.getProject() != null && item.getProject().getId().equals(project.getId()))
                    .filter(item -> item.getName().equals("Legacy Test Case"))
                    .findFirst()
                    .orElseGet(() -> {
                        com.automateit.backend.entity.TestCase item = new com.automateit.backend.entity.TestCase();
                        item.setProject(project);
                        item.setName("Legacy Test Case");
                        item.setMethod("GET");
                        item.setEndpoint("/legacy");
                        item.setDescription("Automatically created for test runs that existed before case relationships.");
                        return testCases.save(item);
                    });
            testRun.setTestCase(legacyCase);
            testRuns.save(testRun);
        });

        var existingAdmin = users.findByUsernameIgnoreCase(normalizedAdminUsername);
        if (existingAdmin.isEmpty() && adminPassword.isBlank()) {
            throw new IllegalStateException("ADMIN_PASSWORD is required when creating the first administrator");
        }
        UserAccount admin = existingAdmin.orElseGet(UserAccount::new);
        admin.setUsername(normalizedAdminUsername);
        admin.setDisplayName(normalizedAdminDisplayName);
        if (!adminPassword.isBlank()
                && (admin.getPasswordHash() == null || !passwordEncoder.matches(adminPassword, admin.getPasswordHash()))) {
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        }
        if (!adminPassword.isBlank()) {
            // ADMIN_PASSWORD is an explicit recovery/bootstrap credential. When it is
            // supplied, the administrator must not remain locked by failures stored in
            // a reused PostgreSQL volume.
            admin.setFailedLoginAttempts(0);
            admin.setLockedUntil(null);
        }
        admin.setRole(UserRole.ADMIN);
        admin.setEnabled(true);
        admin.getProjects().add(defaultProject);
        users.save(admin);
    }

    private String normalizeAdminUsername() {
        String normalized = adminUsername == null ? "" : adminUsername.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || !normalized.matches("[a-z0-9._-]+") || normalized.length() > 100) {
            throw new IllegalStateException("ADMIN_USERNAME must contain only letters, numbers, dot, underscore or hyphen");
        }
        return normalized;
    }

    private String normalizeAdminDisplayName() {
        String normalized = adminDisplayName == null ? "" : adminDisplayName.trim();
        if (normalized.isEmpty() || normalized.length() > 150) {
            throw new IllegalStateException("ADMIN_DISPLAY_NAME must contain between 1 and 150 characters");
        }
        return normalized;
    }

    private void validateBootstrapPassword() {
        if (adminPassword == null || adminPassword.isBlank()) return;
        int byteLength = adminPassword.getBytes(StandardCharsets.UTF_8).length;
        if (adminPassword.length() < 12 || byteLength > 72
                || !adminPassword.matches(".*[a-z].*")
                || !adminPassword.matches(".*[A-Z].*")
                || !adminPassword.matches(".*\\d.*")) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD must contain 12 to 72 bytes, including uppercase, lowercase and a number");
        }
    }
}
