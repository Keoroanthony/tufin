package com.tufin.policyengine.dto;

public record EvaluationRequest(
        String subject,
        String resource,
        String action) {}
