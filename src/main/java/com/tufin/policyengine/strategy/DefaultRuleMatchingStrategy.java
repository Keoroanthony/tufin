package com.tufin.policyengine.strategy;

import com.tufin.policyengine.domain.Rule;
import com.tufin.policyengine.dto.EvaluationRequest;
import org.springframework.stereotype.Component;

@Component
public class DefaultRuleMatchingStrategy implements RuleMatchingStrategy {

    private final ResourceMatchingStrategy resourceMatchingStrategy;

    public DefaultRuleMatchingStrategy(ResourceMatchingStrategy resourceMatchingStrategy) {
        this.resourceMatchingStrategy = resourceMatchingStrategy;
    }

    @Override
    public boolean matches(Rule rule, EvaluationRequest request) {
        return rule.getSubject().equals(request.subject())
                && rule.getAction().equals(request.action())
                && resourceMatchingStrategy.matches(rule.getResource(), request.resource());
    }
}
