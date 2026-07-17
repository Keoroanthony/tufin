package com.tufin.policyengine.service;

import com.tufin.policyengine.domain.Decision;
import com.tufin.policyengine.domain.Rule;
import com.tufin.policyengine.dto.CreateRuleRequest;
import com.tufin.policyengine.dto.RuleResponse;
import com.tufin.policyengine.exception.DuplicateRuleNameException;
import com.tufin.policyengine.exception.RuleNotFoundException;
import com.tufin.policyengine.repository.RuleRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class RuleService {

    private final RuleRepository ruleRepository;

    public RuleService(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public List<RuleResponse> getAllRules() {
        return ruleRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Rule::getPriority).reversed())
                .map(RuleResponse::from)
                .toList();
    }

    public RuleResponse getRuleById(String id) {
        return ruleRepository.findById(id)
                .map(RuleResponse::from)
                .orElseThrow(() -> new RuleNotFoundException(id));
    }

    public RuleResponse createRule(CreateRuleRequest request) {
        if (ruleRepository.existsByName(request.name())) {
            throw new DuplicateRuleNameException(request.name());
        }
        Decision decision = Decision.valueOf(request.decision().toUpperCase());
        Rule rule = new Rule(
                UUID.randomUUID().toString(),
                request.name(),
                request.priority(),
                request.resource(),
                request.action(),
                request.subject(),
                decision,
                request.description(),
                Instant.now());
        return RuleResponse.from(ruleRepository.save(rule));
    }

    public void deleteRule(String id) {
        if (!ruleRepository.deleteById(id)) {
            throw new RuleNotFoundException(id);
        }
    }
}
