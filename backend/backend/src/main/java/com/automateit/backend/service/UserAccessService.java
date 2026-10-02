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
        return users.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user no longer exists"));
    }

    public Project requireProject(Authentication authentication, UUID projectId, boolean write) {
        UserAccount user = currentUser(authentication);
        if (write && !user.getRole().canWriteTests()) {
            throw new AccessDeniedException("Your role has read-only access");
        }
        boolean allowed = user.getRole() == UserRole.ADMIN
                || user.getProjects().stream().anyMatch(project -> project.getId().equals(projectId));
        if (!allowed) {
            throw new AccessDeniedException("You do not have access to this project");
        }
        return projects.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
    }
}
