package com.tufin.policyengine.dto;

public record EvaluationResponse(
        String decision,
        String matchedRuleId,
        String matchedRuleName) {}
