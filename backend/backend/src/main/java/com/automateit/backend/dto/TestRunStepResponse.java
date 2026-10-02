package com.automateit.backend.dto;

import com.automateit.backend.enums.ExecutionItemStatus;
import java.util.UUID;

public record TestRunStepResponse(UUID id, int orderIndex, String description, String expectedResult,
                                  String actualResult, ExecutionItemStatus status) {}
