package com.automateit.backend.repository;

import com.automateit.backend.entity.TestRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TestRunRepository extends JpaRepository<TestRun, UUID>, JpaSpecificationExecutor<TestRun> {
    Page<TestRun> findAllByProjectId(UUID projectId, Pageable pageable);
    boolean existsByTestCaseId(UUID testCaseId);
}
