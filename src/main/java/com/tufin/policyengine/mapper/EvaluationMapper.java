package com.tufin.policyengine.mapper;

import com.tufin.policyengine.domain.Decision;
import com.tufin.policyengine.domain.Rule;
import com.tufin.policyengine.dto.EvaluationHistoryEntry;
import com.tufin.policyengine.dto.EvaluationRequest;
import com.tufin.policyengine.dto.EvaluationResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class EvaluationMapper {

    public EvaluationResponse toResponse(Rule rule) {
        return new EvaluationResponse(rule.getDecision().name(), rule.getId(), rule.getName());
    }

    public EvaluationResponse defaultDeny() {
        return new EvaluationResponse(Decision.DENY.name(), null, null);
    }

    public EvaluationHistoryEntry toHistoryEntry(EvaluationRequest request, EvaluationResponse response) {
        return new EvaluationHistoryEntry(
                Instant.now(),
                request.subject(),
                request.resource(),
                request.action(),
                response.decision(),
                response.matchedRuleId());
    }
}
