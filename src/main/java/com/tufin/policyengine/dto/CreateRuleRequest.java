package com.tufin.policyengine.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateRuleRequest(
        @NotBlank String name,
        @NotNull @Min(1) Integer priority,
        @NotBlank String resource,
        @NotBlank String action,
        @NotBlank String subject,
        @NotBlank @Pattern(regexp = "(?i)^(ALLOW|DENY)$", message = "must be ALLOW or DENY") String decision,
        String description) {}
