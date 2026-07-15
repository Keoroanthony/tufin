package com.tufin.policyengine.controller;

import com.tufin.policyengine.dto.EvaluationHistoryEntry;
import com.tufin.policyengine.dto.EvaluationRequest;
import com.tufin.policyengine.dto.EvaluationResponse;
import com.tufin.policyengine.service.PolicyRuleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/evaluate")
public class EvaluationController {

    private final PolicyRuleService policyRuleService;

    public EvaluationController(PolicyRuleService policyRuleService) {
        this.policyRuleService = policyRuleService;
    }

    @PostMapping
    public ResponseEntity<EvaluationResponse> evaluate(@Valid @RequestBody EvaluationRequest request) {
        return ResponseEntity.ok(policyRuleService.evaluateTraffic(request));
    }

    @GetMapping("/history")
    public ResponseEntity<List<EvaluationHistoryEntry>> history() {
        return ResponseEntity.ok(policyRuleService.getEvaluationHistory());
    }
}
