package com.tufin.policyengine.strategy;

import com.tufin.policyengine.domain.Rule;
import com.tufin.policyengine.dto.EvaluationRequest;

public interface RuleMatchingStrategy {

    boolean matches(Rule rule, EvaluationRequest request);
}
