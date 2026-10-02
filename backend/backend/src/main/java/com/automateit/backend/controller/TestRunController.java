package com.automateit.backend.controller;

import com.automateit.backend.dto.*;
import com.automateit.backend.enums.EnvironmentType;
import com.automateit.backend.enums.ExecutionType;
import com.automateit.backend.enums.TestRunStatus;
import com.automateit.backend.service.TestRunService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequestMapping("/api/test-runs")
public class TestRunController {

    private final TestRunService testRunService;

    public TestRunController(TestRunService testRunService) {
        this.testRunService = testRunService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestRunResponse create(@Valid @RequestBody CreateTestRunRequest request, Authentication authentication) {
        return testRunService.create(request, authentication);
    }

    @GetMapping
    public Page<TestRunResponse> findAll(
            @RequestParam UUID projectId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TestRunStatus status,
            @RequestParam(required = false) EnvironmentType environment,
            @RequestParam(required = false) ExecutionType executionType,
            @RequestParam(required = false) UUID testCaseId,
            @RequestParam(defaultValue = "false") boolean finalOnly,
            @PageableDefault(size = 20, sort = "createdAt", direction = DESC) Pageable pageable,
            Authentication authentication
    ) {
        return testRunService.findAll(projectId, q, status, environment, executionType,
                testCaseId, finalOnly, pageable, authentication);
    }

    @PutMapping("/{id}")
    public TestRunResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTestRunRequest request,
                                  Authentication authentication) {
        return testRunService.update(id, request, authentication);
    }

    @PatchMapping("/{id}/details")
    public TestRunResponse updateDetails(@PathVariable UUID id,
                                         @Valid @RequestBody UpdateTestRunDetailsRequest request,
                                         Authentication authentication) {
        return testRunService.updateDetails(id, request, authentication);
    }

    @GetMapping("/{id}")
    public TestRunResponse findById(@PathVariable UUID id, Authentication authentication) {
        return testRunService.findById(id, authentication);
    }

    @PatchMapping("/{id}/status")
    public TestRunResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTestRunStatusRequest request,
            Authentication authentication
    ) {
        return testRunService.updateStatus(id, request, authentication);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication authentication) {
        testRunService.delete(id, authentication);
    }
}
