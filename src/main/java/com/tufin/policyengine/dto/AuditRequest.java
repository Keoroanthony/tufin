package com.tufin.policyengine.dto;

public record AuditRequest(
        String subject,
        String resource,
        String action,
        String decision,
        String matchedRuleId,
        String reason) {}
