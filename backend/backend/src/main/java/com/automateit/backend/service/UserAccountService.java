package com.automateit.backend.service;

import com.automateit.backend.dto.*;
import com.automateit.backend.entity.Project;
import com.automateit.backend.entity.UserAccount;
import com.automateit.backend.enums.UserRole;
import com.automateit.backend.exception.ConflictException;
import com.automateit.backend.exception.ResourceNotFoundException;
import com.automateit.backend.repository.ProjectRepository;
import com.automateit.backend.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserAccountService {

    private final UserAccountRepository users;
    private final ProjectRepository projects;
    private final PasswordEncoder passwordEncoder;
    private final ProjectService projectService;

    public UserAccountService(UserAccountRepository users, ProjectRepository projects,
                              PasswordEncoder passwordEncoder, ProjectService projectService) {
        this.users = users;
        this.projects = projects;
        this.passwordEncoder = passwordEncoder;
        this.projectService = projectService;
    }

    public List<UserResponse> findAll() {
        return users.findAll().stream().sorted(Comparator.comparing(UserAccount::getUsername)).map(this::toResponse).toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String username = request.username().trim().toLowerCase();
        if (users.existsByUsernameIgnoreCase(username)) throw new ConflictException("Username already exists");
        LinkedHashSet<Project> assigned = new LinkedHashSet<>(projects.findAllById(request.projectIds()));
        if (assigned.size() != request.projectIds().size()) {
            throw new ResourceNotFoundException("One or more projects were not found");
        }
        if (request.role() != UserRole.ADMIN && assigned.isEmpty()) {
            throw new IllegalArgumentException("At least one project is required for non-admin users");
        }
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setDisplayName(request.displayName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setProjects(assigned);
        return toResponse(users.save(user));
    }

    public AuthUserResponse toAuthResponse(UserAccount user, List<ProjectResponse> accessibleProjects) {
        return new AuthUserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(), accessibleProjects);
    }

    private UserResponse toResponse(UserAccount user) {
        List<ProjectResponse> assigned = user.getProjects().stream()
                .sorted(Comparator.comparing(Project::getName)).map(projectService::toResponse).toList();
        return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(),
                user.isEnabled(), assigned, user.getCreatedAt());
    }
}
