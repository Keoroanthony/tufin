package com.tufin.policyengine.dto;

import java.time.Instant;

public record EvaluationHistoryEntry(
        Instant timestamp,
        String subject,
        String resource,
        String action,
        String decision,
        String matchedRuleId) {}
