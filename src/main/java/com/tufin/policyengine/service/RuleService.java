package com.tufin.policyengine.service;

import com.tufin.policyengine.dto.RuleResponse;
import com.tufin.policyengine.exception.RuleNotFoundException;
import com.tufin.policyengine.repository.RuleRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RuleService {

    private final RuleRepository ruleRepository;

    public RuleService(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public List<RuleResponse> getAllRules() {
        return ruleRepository.findAll().stream()
                .sorted(Comparator.comparingInt(r -> -r.getPriority()))
                .map(RuleResponse::from)
                .collect(Collectors.toList());
    }

    public RuleResponse getRuleById(String id) {
        return ruleRepository.findById(id)
                .map(RuleResponse::from)
                .orElseThrow(() -> new RuleNotFoundException(id));
    }
}
