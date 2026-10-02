package com.automateit.backend.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "test_assertions")
public class TestAssertion {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_run_id", nullable = false)
    private TestRun testRun;

    @Column(nullable = false, length = 300)
    private String name;

    @Column(name = "expected_value", length = 2000)
    private String expectedValue;

    @Column(name = "actual_value", length = 2000)
    private String actualValue;

    @Column(nullable = false)
    private boolean passed;

    public UUID getId() { return id; }
    public TestRun getTestRun() { return testRun; }
    public void setTestRun(TestRun testRun) { this.testRun = testRun; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getExpectedValue() { return expectedValue; }
    public void setExpectedValue(String expectedValue) { this.expectedValue = expectedValue; }
    public String getActualValue() { return actualValue; }
    public void setActualValue(String actualValue) { this.actualValue = actualValue; }
    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }
}
