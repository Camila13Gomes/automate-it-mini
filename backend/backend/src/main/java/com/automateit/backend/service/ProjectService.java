package com.automateit.backend.service;

import com.automateit.backend.dto.CreateProjectRequest;
import com.automateit.backend.dto.ProjectResponse;
import com.automateit.backend.entity.Project;
import com.automateit.backend.entity.UserAccount;
import com.automateit.backend.enums.UserRole;
import com.automateit.backend.exception.ConflictException;
import com.automateit.backend.repository.ProjectRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projects;
    private final UserAccessService access;

    public ProjectService(ProjectRepository projects, UserAccessService access) {
        this.projects = projects;
        this.access = access;
    }

    public List<ProjectResponse> findAccessible(Authentication authentication) {
        UserAccount user = access.currentUser(authentication);
        List<Project> accessible = user.getRole() == UserRole.ADMIN ? projects.findAll() : List.copyOf(user.getProjects());
        return accessible.stream().sorted(Comparator.comparing(Project::getName)).map(this::toResponse).toList();
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        String slug = request.slug().trim().toLowerCase();
        if (projects.existsBySlug(slug)) throw new ConflictException("Project slug already exists");
        Project project = new Project();
        project.setName(request.name().trim());
        project.setSlug(slug);
        return toResponse(projects.save(project));
    }

    public ProjectResponse toResponse(Project project) {
        return new ProjectResponse(project.getId(), project.getName(), project.getSlug(), project.getCreatedAt());
    }
}
