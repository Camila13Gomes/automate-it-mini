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

        var existingAdmin = users.findByUsernameIgnoreCase(adminUsername);
        if (existingAdmin.isEmpty() && adminPassword.isBlank()) {
            throw new IllegalStateException("ADMIN_PASSWORD is required when creating the first administrator");
        }
        UserAccount admin = existingAdmin.orElseGet(UserAccount::new);
        admin.setUsername(adminUsername.trim().toLowerCase());
        admin.setDisplayName(adminDisplayName.trim());
        if (!adminPassword.isBlank()
                && (admin.getPasswordHash() == null || !passwordEncoder.matches(adminPassword, admin.getPasswordHash()))) {
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        }
        admin.setRole(UserRole.ADMIN);
        admin.setEnabled(true);
        admin.getProjects().add(defaultProject);
        users.save(admin);
    }
}
