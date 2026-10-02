package com.automateit.backend.service;

import com.automateit.backend.dto.*;
import com.automateit.backend.entity.TestAssertion;
import com.automateit.backend.entity.TestCase;
import com.automateit.backend.entity.TestRun;
import com.automateit.backend.entity.TestRunStep;
import com.automateit.backend.enums.EnvironmentType;
import com.automateit.backend.enums.ExecutionType;
import com.automateit.backend.enums.TestRunStatus;
import com.automateit.backend.exception.ConflictException;
import com.automateit.backend.exception.InvalidStatusTransitionException;
import com.automateit.backend.exception.ResourceNotFoundException;
import com.automateit.backend.repository.TestCaseRepository;
import com.automateit.backend.repository.TestRunRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TestRunService {

    private static final Map<TestRunStatus, Set<TestRunStatus>> ALLOWED_TRANSITIONS = Map.of(
            TestRunStatus.PENDING, EnumSet.of(TestRunStatus.RUNNING, TestRunStatus.CANCELLED),
            TestRunStatus.RUNNING, EnumSet.of(TestRunStatus.PASSED, TestRunStatus.FAILED, TestRunStatus.CANCELLED),
            TestRunStatus.PASSED, EnumSet.noneOf(TestRunStatus.class),
            TestRunStatus.FAILED, EnumSet.noneOf(TestRunStatus.class),
            TestRunStatus.CANCELLED, EnumSet.noneOf(TestRunStatus.class)
    );

    private final TestRunRepository testRunRepository;
    private final TestCaseRepository testCaseRepository;
    private final UserAccessService access;

    public TestRunService(TestRunRepository testRunRepository, TestCaseRepository testCaseRepository,
                          UserAccessService access) {
        this.testRunRepository = testRunRepository;
        this.testCaseRepository = testCaseRepository;
        this.access = access;
    }

    @Transactional
    public TestRunResponse create(CreateTestRunRequest request, Authentication authentication) {
        var project = access.requireProject(authentication, request.getProjectId(), true);
        var user = access.currentUser(authentication);
        TestCase testCase = requireTestCase(request.getProjectId(), request.getTestCaseId());
        TestRun testRun = new TestRun();
        testRun.setProject(project);
        testRun.setTestCase(testCase);
        testRun.setName(request.getName().trim());
        testRun.setProjectName(project.getName());
        testRun.setEnvironment(request.getEnvironment());
        testRun.setExecutionType(request.getExecutionType());
        testRun.setStatus(TestRunStatus.PENDING);
        testRun.setCreatedBy(user.getDisplayName());
        testRun.setNotes(normalizeOptionalText(request.getNotes()));
        return toResponse(testRunRepository.save(testRun));
    }

    public Page<TestRunResponse> findAll(UUID projectId, String q, TestRunStatus status,
                                         EnvironmentType environment, ExecutionType executionType,
                                         UUID testCaseId, boolean finalOnly, Pageable pageable, Authentication authentication) {
        access.requireProject(authentication, projectId, false);
        Specification<TestRun> spec = (root, query, builder) -> builder.equal(root.get("project").get("id"), projectId);
        if (q != null && !q.isBlank()) {
            String term = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("name")), term),
                    builder.like(builder.lower(root.get("notes")), term),
                    builder.like(builder.lower(root.get("createdBy")), term),
                    builder.like(builder.lower(root.get("testCase").get("name")), term)
            ));
        }
        if (status != null) spec = spec.and((root, query, builder) -> builder.equal(root.get("status"), status));
        if (environment != null) spec = spec.and((root, query, builder) -> builder.equal(root.get("environment"), environment));
        if (executionType != null) spec = spec.and((root, query, builder) -> builder.equal(root.get("executionType"), executionType));
        if (testCaseId != null) spec = spec.and((root, query, builder) -> builder.equal(root.get("testCase").get("id"), testCaseId));
        if (finalOnly) spec = spec.and((root, query, builder) -> root.get("status").in(
                TestRunStatus.PASSED, TestRunStatus.FAILED, TestRunStatus.CANCELLED));
        return testRunRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public TestRunResponse findById(UUID id, Authentication authentication) {
        TestRun run = findEntityById(id);
        access.requireProject(authentication, run.getProject().getId(), false);
        return toResponse(run);
    }

    @Transactional
    public TestRunResponse update(UUID id, UpdateTestRunRequest request, Authentication authentication) {
        TestRun run = findEntityById(id);
        access.requireProject(authentication, run.getProject().getId(), true);
        if (isFinalStatus(run.getStatus())) throw new ConflictException("Finished test runs cannot be edited");
        TestCase testCase = requireTestCase(run.getProject().getId(), request.testCaseId());
        run.setName(request.name().trim());
        run.setTestCase(testCase);
        run.setEnvironment(request.environment());
        run.setExecutionType(request.executionType());
        run.setNotes(normalizeOptionalText(request.notes()));
        return toResponse(testRunRepository.save(run));
    }

    @Transactional
    public TestRunResponse updateDetails(UUID id, UpdateTestRunDetailsRequest request, Authentication authentication) {
        TestRun run = findEntityById(id);
        access.requireProject(authentication, run.getProject().getId(), true);
        run.setResponseBody(normalizeOptionalText(request.responseBody()));
        run.setEvidence(normalizeOptionalText(request.evidence()));
        run.setLogs(normalizeOptionalText(request.logs()));
        run.getSteps().clear();
        if (request.steps() != null) {
            for (int index = 0; index < request.steps().size(); index++) {
                TestRunStepRequest item = request.steps().get(index);
                TestRunStep step = new TestRunStep();
                step.setTestRun(run);
                step.setOrderIndex(index + 1);
                step.setDescription(item.description().trim());
                step.setExpectedResult(normalizeOptionalText(item.expectedResult()));
                step.setActualResult(normalizeOptionalText(item.actualResult()));
                step.setStatus(item.status());
                run.getSteps().add(step);
            }
        }
        run.getAssertions().clear();
        if (request.assertions() != null) {
            request.assertions().forEach(item -> {
                TestAssertion assertion = new TestAssertion();
                assertion.setTestRun(run);
                assertion.setName(item.name().trim());
                assertion.setExpectedValue(normalizeOptionalText(item.expectedValue()));
                assertion.setActualValue(normalizeOptionalText(item.actualValue()));
                assertion.setPassed(item.passed());
                run.getAssertions().add(assertion);
            });
        }
        return toResponse(testRunRepository.save(run));
    }

    @Transactional
    public TestRunResponse updateStatus(UUID id, UpdateTestRunStatusRequest request, Authentication authentication) {
        TestRun run = findEntityById(id);
        access.requireProject(authentication, run.getProject().getId(), true);
        TestRunStatus currentStatus = run.getStatus();
        TestRunStatus requestedStatus = request.getStatus();
        validateTransition(currentStatus, requestedStatus);
        if (currentStatus != requestedStatus) {
            LocalDateTime now = LocalDateTime.now();
            if (requestedStatus == TestRunStatus.RUNNING && run.getStartedAt() == null) run.setStartedAt(now);
            if (isFinalStatus(requestedStatus) && run.getFinishedAt() == null) {
                run.setFinishedAt(now);
                run.setFinished(true);
                run.setDurationMs(run.getStartedAt() == null ? 0L : Duration.between(run.getStartedAt(), now).toMillis());
            }
            run.setStatus(requestedStatus);
        }
        return toResponse(testRunRepository.save(run));
    }

    @Transactional
    public void delete(UUID id, Authentication authentication) {
        TestRun run = findEntityById(id);
        access.requireProject(authentication, run.getProject().getId(), true);
        testRunRepository.delete(run);
    }

    private TestCase requireTestCase(UUID projectId, UUID testCaseId) {
        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found with id: " + testCaseId));
        if (!testCase.getProject().getId().equals(projectId)) {
            throw new ConflictException("Test case belongs to another project");
        }
        return testCase;
    }

    private TestRun findEntityById(UUID id) {
        return testRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test run not found with id: " + id));
    }

    private void validateTransition(TestRunStatus currentStatus, TestRunStatus requestedStatus) {
        if (currentStatus == requestedStatus) return;
        if (!ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(requestedStatus)) {
            throw new InvalidStatusTransitionException(
                    "Invalid test run status transition from " + currentStatus + " to " + requestedStatus);
        }
    }

    private boolean isFinalStatus(TestRunStatus status) {
        return status == TestRunStatus.PASSED || status == TestRunStatus.FAILED || status == TestRunStatus.CANCELLED;
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private TestRunResponse toResponse(TestRun run) {
        TestRunResponse response = new TestRunResponse();
        response.setId(run.getId());
        response.setProjectId(run.getProject().getId());
        response.setTestCaseId(run.getTestCase() == null ? null : run.getTestCase().getId());
        response.setTestCaseName(run.getTestCase() == null ? "Legacy test case" : run.getTestCase().getName());
        response.setName(run.getName());
        response.setProjectName(run.getProjectName());
        response.setEnvironment(run.getEnvironment());
        response.setExecutionType(run.getExecutionType());
        response.setStatus(run.getStatus());
        response.setCreatedBy(run.getCreatedBy());
        response.setNotes(run.getNotes());
        response.setCreatedAt(run.getCreatedAt());
        response.setUpdatedAt(run.getUpdatedAt());
        response.setStartedAt(run.getStartedAt());
        response.setFinishedAt(run.getFinishedAt());
        response.setResponseBody(run.getResponseBody());
        response.setEvidence(run.getEvidence());
        response.setLogs(run.getLogs());
        response.setDurationMs(run.getDurationMs());
        response.setSteps(run.getSteps().stream().map(step -> new TestRunStepResponse(step.getId(),
                step.getOrderIndex(), step.getDescription(), step.getExpectedResult(), step.getActualResult(), step.getStatus())).toList());
        response.setAssertions(run.getAssertions().stream().map(assertion -> new TestAssertionResponse(assertion.getId(),
                assertion.getName(), assertion.getExpectedValue(), assertion.getActualValue(), assertion.isPassed())).toList());
        return response;
    }
}
