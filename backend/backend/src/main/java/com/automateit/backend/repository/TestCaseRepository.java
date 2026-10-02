package com.automateit.backend.repository;

import com.automateit.backend.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TestCaseRepository extends JpaRepository<TestCase, UUID>, JpaSpecificationExecutor<TestCase> {
    Page<TestCase> findAllByProjectId(UUID projectId, Pageable pageable);
    boolean existsByProjectIdAndId(UUID projectId, UUID id);
}
