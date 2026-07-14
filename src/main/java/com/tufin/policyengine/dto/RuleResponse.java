package com.tufin.policyengine.dto;

import com.tufin.policyengine.domain.Rule;

import java.time.Instant;

public record RuleResponse(
        String id,
        String name,
        int priority,
        String resource,
        String action,
        String subject,
        String decision,
        String description,
        Instant createdAt) {

    public static RuleResponse from(Rule rule) {
        return new RuleResponse(
                rule.getId(),
                rule.getName(),
                rule.getPriority(),
                rule.getResource(),
                rule.getAction(),
                rule.getSubject(),
                rule.getDecision().name(),
                rule.getDescription(),
                rule.getCreatedAt());
    }
}
