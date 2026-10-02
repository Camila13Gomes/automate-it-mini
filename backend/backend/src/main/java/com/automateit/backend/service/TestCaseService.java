package com.automateit.backend.service;

import com.automateit.backend.dto.CreateTestCaseRequest;
import com.automateit.backend.dto.TestCaseResponse;
import com.automateit.backend.dto.UpdateTestCaseRequest;
import com.automateit.backend.entity.TestCase;
import com.automateit.backend.exception.ResourceNotFoundException;
import com.automateit.backend.repository.TestCaseRepository;
import com.automateit.backend.repository.TestRunRepository;
import com.automateit.backend.exception.ConflictException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final UserAccessService access;
    private final TestRunRepository testRunRepository;

    public TestCaseService(TestCaseRepository testCaseRepository, UserAccessService access,
                           TestRunRepository testRunRepository) {
        this.testCaseRepository = testCaseRepository;
        this.access = access;
        this.testRunRepository = testRunRepository;
    }

    @Transactional
    public TestCaseResponse create(CreateTestCaseRequest request, Authentication authentication) {
        TestCase testCase = new TestCase();
        testCase.setProject(access.requireProject(authentication, request.getProjectId(), true));
        testCase.setName(request.getName().trim());
        testCase.setMethod(request.getMethod().trim().toUpperCase(Locale.ROOT));
        testCase.setEndpoint(request.getEndpoint().trim());
        testCase.setDescription(normalizeOptionalText(request.getDescription()));

        return toResponse(testCaseRepository.save(testCase));
    }

    public Page<TestCaseResponse> findAll(UUID projectId, String q, String method, Pageable pageable,
                                          Authentication authentication) {
        access.requireProject(authentication, projectId, false);
        Specification<TestCase> spec = (root, query, builder) -> builder.equal(root.get("project").get("id"), projectId);
        if (q != null && !q.isBlank()) {
            String term = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("name")), term),
                    builder.like(builder.lower(root.get("endpoint")), term),
                    builder.like(builder.lower(root.get("description")), term)
            ));
        }
        if (method != null && !method.isBlank()) {
            spec = spec.and((root, query, builder) -> builder.equal(root.get("method"), method.toUpperCase(Locale.ROOT)));
        }
        return testCaseRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional
    public TestCaseResponse update(UUID id, UpdateTestCaseRequest request, Authentication authentication) {
        TestCase testCase = findEntity(id);
        access.requireProject(authentication, testCase.getProject().getId(), true);
        testCase.setName(request.name().trim());
        testCase.setMethod(request.method().trim().toUpperCase(Locale.ROOT));
        testCase.setEndpoint(request.endpoint().trim());
        testCase.setDescription(normalizeOptionalText(request.description()));
        return toResponse(testCaseRepository.save(testCase));
    }

    @Transactional
    public void delete(UUID id, Authentication authentication) {
        TestCase testCase = findEntity(id);
        access.requireProject(authentication, testCase.getProject().getId(), true);
        if (testRunRepository.existsByTestCaseId(id)) {
            throw new ConflictException("Test case cannot be deleted because it has test runs");
        }
        testCaseRepository.delete(testCase);
    }

    private TestCase findEntity(UUID id) {
        return testCaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found with id: " + id));
    }

    public TestCaseResponse findById(UUID id, Authentication authentication) {
        TestCase testCase = testCaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found with id: " + id));
        access.requireProject(authentication, testCase.getProject().getId(), false);

        return toResponse(testCase);
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private TestCaseResponse toResponse(TestCase testCase) {
        TestCaseResponse response = new TestCaseResponse();
        response.setId(testCase.getId());
        response.setProjectId(testCase.getProject().getId());
        response.setProjectName(testCase.getProject().getName());
        response.setName(testCase.getName());
        response.setMethod(testCase.getMethod());
        response.setEndpoint(testCase.getEndpoint());
        response.setDescription(testCase.getDescription());
        response.setCreatedAt(testCase.getCreatedAt());
        response.setUpdatedAt(testCase.getUpdatedAt());
        return response;
    }
}
