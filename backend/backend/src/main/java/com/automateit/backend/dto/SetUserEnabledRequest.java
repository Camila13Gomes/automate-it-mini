package com.automateit.backend.dto;

import jakarta.validation.constraints.NotNull;

public record SetUserEnabledRequest(
        @NotNull(message = "Enabled is required") Boolean enabled
) {}
