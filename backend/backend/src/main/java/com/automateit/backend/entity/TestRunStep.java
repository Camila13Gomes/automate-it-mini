package com.automateit.backend.entity;

import com.automateit.backend.enums.ExecutionItemStatus;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "test_run_steps")
public class TestRunStep {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_run_id", nullable = false)
    private TestRun testRun;

    @Column(name = "step_order", nullable = false)
    private int orderIndex;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "expected_result", length = 2000)
    private String expectedResult;

    @Column(name = "actual_result", length = 2000)
    private String actualResult;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExecutionItemStatus status = ExecutionItemStatus.PENDING;

    public UUID getId() { return id; }
    public TestRun getTestRun() { return testRun; }
    public void setTestRun(TestRun testRun) { this.testRun = testRun; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getExpectedResult() { return expectedResult; }
    public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }
    public String getActualResult() { return actualResult; }
    public void setActualResult(String actualResult) { this.actualResult = actualResult; }
    public ExecutionItemStatus getStatus() { return status; }
    public void setStatus(ExecutionItemStatus status) { this.status = status; }
}
