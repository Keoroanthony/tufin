package com.tufin.policyengine.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRuleRequest(
        @NotBlank String name,
        @NotNull @Min(1) Integer priority,
        @NotBlank String resource,
        @NotBlank String action,
        @NotBlank String subject,
        @NotBlank String decision,
        String description) {}
