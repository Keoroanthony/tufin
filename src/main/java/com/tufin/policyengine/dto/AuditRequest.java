package com.tufin.policyengine.dto;

import java.time.Instant;

public record AuditRequest(
        String subject,
        String resource,
        String action,
        String decision,
        String matchedRuleId,
        Instant timestamp) {}
