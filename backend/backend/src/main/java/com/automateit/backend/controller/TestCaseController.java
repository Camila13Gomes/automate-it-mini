package com.automateit.backend.controller;

import com.automateit.backend.dto.CreateTestCaseRequest;
import com.automateit.backend.dto.TestCaseResponse;
import com.automateit.backend.dto.UpdateTestCaseRequest;
import com.automateit.backend.service.TestCaseService;
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
@RequestMapping("/api/test-cases")
public class TestCaseController {

    private final TestCaseService testCaseService;

    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestCaseResponse create(@Valid @RequestBody CreateTestCaseRequest request, Authentication authentication) {
        return testCaseService.create(request, authentication);
    }

    @GetMapping
    public Page<TestCaseResponse> findAll(
            @RequestParam UUID projectId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String method,
            @PageableDefault(size = 20, sort = "createdAt", direction = DESC) Pageable pageable,
            Authentication authentication
    ) {
        return testCaseService.findAll(projectId, q, method, pageable, authentication);
    }

    @GetMapping("/{id}")
    public TestCaseResponse findById(@PathVariable UUID id, Authentication authentication) {
        return testCaseService.findById(id, authentication);
    }

    @PutMapping("/{id}")
    public TestCaseResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTestCaseRequest request,
                                   Authentication authentication) {
        return testCaseService.update(id, request, authentication);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication authentication) {
        testCaseService.delete(id, authentication);
    }
}
