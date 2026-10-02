package com.automateit.backend.dto;

import java.util.UUID;

public record TestAssertionResponse(UUID id, String name, String expectedValue, String actualValue, boolean passed) {}
