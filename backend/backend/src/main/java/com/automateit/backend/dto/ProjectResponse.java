package com.automateit.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectResponse(UUID id, String name, String slug, LocalDateTime createdAt) {}
