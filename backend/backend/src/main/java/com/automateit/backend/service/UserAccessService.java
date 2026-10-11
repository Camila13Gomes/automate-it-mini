package com.automateit.backend.service;

import com.automateit.backend.entity.Project;
import com.automateit.backend.entity.UserAccount;
import com.automateit.backend.enums.UserRole;
import com.automateit.backend.exception.ResourceNotFoundException;
import com.automateit.backend.repository.ProjectRepository;
import com.automateit.backend.repository.UserAccountRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserAccessService {

    private final UserAccountRepository users;
    private final ProjectRepository projects;

    public UserAccessService(UserAccountRepository users, ProjectRepository projects) {
        this.users = users;
        this.projects = projects;
    }

    public UserAccount currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        UserAccount user = users.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user no longer exists"));
        if (!user.isEnabled() || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now()))) {
            throw new AccessDeniedException("User account is disabled or locked");
        }
        return user;
    }

    public Project requireProject(Authentication authentication, UUID projectId) {
        return requireProjectAccess(authentication, projectId, AccessLevel.READ);
    }

    public Project requireProjectForManagement(Authentication authentication, UUID projectId) {
        return requireProjectAccess(authentication, projectId, AccessLevel.MANAGE);
    }

    public Project requireProjectForExecution(Authentication authentication, UUID projectId) {
        return requireProjectAccess(authentication, projectId, AccessLevel.EXECUTE);
    }

    private Project requireProjectAccess(Authentication authentication, UUID projectId, AccessLevel level) {
        UserAccount user = currentUser(authentication);
        if (level == AccessLevel.MANAGE && !user.getRole().canManageTests()) {
            throw new AccessDeniedException("Your role cannot manage tests");
        }
        if (level == AccessLevel.EXECUTE && !user.getRole().canExecuteTests()) {
            throw new AccessDeniedException("Your role cannot execute tests");
        }
        boolean allowed = user.getRole() == UserRole.ADMIN
                || user.getProjects().stream().anyMatch(project -> project.getId().equals(projectId));
        if (!allowed) {
            throw new AccessDeniedException("You do not have access to this project");
        }
        return projects.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
    }

    private enum AccessLevel { READ, MANAGE, EXECUTE }
}
