package com.tufin.policyengine.dto;

public record CreateRuleRequest(
        String name,
        Integer priority,
        String resource,
        String action,
        String subject,
        String decision,
        String description) {}
