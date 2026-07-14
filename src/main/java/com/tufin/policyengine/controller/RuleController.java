package com.tufin.policyengine.controller;

import com.tufin.policyengine.dto.RuleResponse;
import com.tufin.policyengine.service.RuleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rules")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    public ResponseEntity<List<RuleResponse>> getAllRules() {
        return ResponseEntity.ok(ruleService.getAllRules());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RuleResponse> getRuleById(@PathVariable String id) {
        return ResponseEntity.ok(ruleService.getRuleById(id));
    }
}
