package com.tufin.policyengine.service.impl;

import com.tufin.policyengine.domain.Rule;
import com.tufin.policyengine.dto.EvaluationHistoryEntry;
import com.tufin.policyengine.dto.EvaluationRequest;
import com.tufin.policyengine.dto.EvaluationResponse;
import com.tufin.policyengine.history.EvaluationHistoryStore;
import com.tufin.policyengine.mapper.EvaluationMapper;
import com.tufin.policyengine.repository.RuleRepository;
import com.tufin.policyengine.service.PolicyRuleService;
import com.tufin.policyengine.strategy.RuleMatchingStrategy;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class PolicyRuleServiceImpl implements PolicyRuleService {

    private final RuleRepository ruleRepository;
    private final RuleMatchingStrategy matchingStrategy;
    private final EvaluationMapper mapper;
    private final EvaluationHistoryStore historyStore;

    public PolicyRuleServiceImpl(
            RuleRepository ruleRepository,
            RuleMatchingStrategy matchingStrategy,
            EvaluationMapper mapper,
            EvaluationHistoryStore historyStore) {
        this.ruleRepository = ruleRepository;
        this.matchingStrategy = matchingStrategy;
        this.mapper = mapper;
        this.historyStore = historyStore;
    }

    @Override
    public EvaluationResponse evaluateTraffic(EvaluationRequest request) {
        EvaluationResponse response = ruleRepository.findAll().stream()
                .filter(rule -> matchingStrategy.matches(rule, request))
                .sorted(Comparator.comparingInt(Rule::getPriority).reversed()
                        .thenComparing(Rule::getCreatedAt))
                .findFirst()
                .map(mapper::toResponse)
                .orElseGet(mapper::defaultDeny);

        historyStore.add(mapper.toHistoryEntry(request, response));
        return response;
    }

    @Override
    public List<EvaluationHistoryEntry> getEvaluationHistory() {
        return historyStore.getAll();
    }
}
