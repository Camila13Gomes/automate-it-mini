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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

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

    public List<UserResponse> findAll(Authentication authentication) {
        requireActiveAdmin(authentication);
        return users.findAll().stream().sorted(Comparator.comparing(UserAccount::getUsername)).map(this::toResponse).toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest request, Authentication authentication) {
        requireActiveAdmin(authentication);
        validatePasswordLength(request.password());
        String username = normalizeUsername(request.username());
        if (users.existsByUsernameIgnoreCase(username)) throw new ConflictException("Username already exists");
        LinkedHashSet<Project> assigned = resolveProjects(request.projectIds(), request.role());
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setDisplayName(request.displayName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setProjects(assigned);
        return toResponse(users.save(user));
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request, Authentication authentication) {
        UserAccount actor = requireActiveAdmin(authentication);
        UserAccount user = findById(id);
        String username = normalizeUsername(request.username());
        boolean self = actor.getId().equals(user.getId());

        users.findByUsernameIgnoreCase(username)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> { throw new ConflictException("Username already exists"); });
        if (self && !user.getUsername().equalsIgnoreCase(username)) {
            throw new ConflictException("You cannot change your own username while signed in");
        }
        if (self && request.role() != UserRole.ADMIN) {
            throw new ConflictException("You cannot remove your own administrator role");
        }
        if (user.getRole() == UserRole.ADMIN && user.isEnabled() && request.role() != UserRole.ADMIN) {
            ensureAnotherActiveAdmin();
        }

        user.setUsername(username);
        user.setDisplayName(request.displayName().trim());
        user.setRole(request.role());
        user.setProjects(resolveProjects(request.projectIds(), request.role()));
        return toResponse(users.save(user));
    }

    @Transactional
    public UserResponse setEnabled(UUID id, SetUserEnabledRequest request, Authentication authentication) {
        UserAccount actor = requireActiveAdmin(authentication);
        UserAccount user = findById(id);
        boolean enabled = request.enabled();
        if (!enabled && actor.getId().equals(user.getId())) {
            throw new ConflictException("You cannot disable your own account");
        }
        if (!enabled && user.isEnabled() && user.getRole() == UserRole.ADMIN) {
            ensureAnotherActiveAdmin();
        }
        user.setEnabled(enabled);
        if (enabled) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        }
        return toResponse(users.save(user));
    }

    @Transactional
    public void resetPassword(UUID id, ResetUserPasswordRequest request, Authentication authentication) {
        requireActiveAdmin(authentication);
        validatePasswordLength(request.password());
        UserAccount user = findById(id);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        users.save(user);
    }

    @Transactional
    public void delete(UUID id, Authentication authentication) {
        UserAccount actor = requireActiveAdmin(authentication);
        UserAccount user = findById(id);
        if (actor.getId().equals(user.getId())) {
            throw new ConflictException("You cannot delete your own account");
        }
        if (user.getRole() == UserRole.ADMIN && user.isEnabled()) {
            ensureAnotherActiveAdmin();
        }
        users.delete(user);
    }

    public AuthUserResponse toAuthResponse(UserAccount user, List<ProjectResponse> accessibleProjects) {
        return new AuthUserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(), accessibleProjects);
    }

    private UserAccount requireActiveAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        UserAccount actor = users.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user no longer exists"));
        boolean locked = actor.getLockedUntil() != null && actor.getLockedUntil().isAfter(LocalDateTime.now());
        if (!actor.isEnabled() || locked || actor.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Active administrator access required");
        }
        return actor;
    }

    private UserAccount findById(UUID id) {
        return users.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private LinkedHashSet<Project> resolveProjects(Set<UUID> projectIds, UserRole role) {
        LinkedHashSet<Project> assigned = new LinkedHashSet<>(projects.findAllById(projectIds));
        if (assigned.size() != projectIds.size()) {
            throw new ResourceNotFoundException("One or more projects were not found");
        }
        if (role != UserRole.ADMIN && assigned.isEmpty()) {
            throw new IllegalArgumentException("At least one project is required for non-admin users");
        }
        return assigned;
    }

    private void ensureAnotherActiveAdmin() {
        if (users.countByRoleAndEnabledTrue(UserRole.ADMIN) <= 1) {
            throw new ConflictException("At least one active administrator must remain");
        }
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private void validatePasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must contain at most 72 UTF-8 bytes");
        }
    }

    private UserResponse toResponse(UserAccount user) {
        List<ProjectResponse> assigned = user.getProjects().stream()
                .sorted(Comparator.comparing(Project::getName)).map(projectService::toResponse).toList();
        return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(),
                user.isEnabled(), assigned, user.getCreatedAt());
    }
}
