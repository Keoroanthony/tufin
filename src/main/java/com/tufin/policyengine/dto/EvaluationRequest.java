package com.tufin.policyengine.dto;

import jakarta.validation.constraints.NotBlank;

public record EvaluationRequest(
        @NotBlank String subject,
        @NotBlank String resource,
        @NotBlank String action) {}
