package com.tufin.policyengine.service;

import com.tufin.policyengine.dto.EvaluationHistoryEntry;
import com.tufin.policyengine.dto.EvaluationRequest;
import com.tufin.policyengine.dto.EvaluationResponse;

import java.util.List;

public interface PolicyRuleService {

    EvaluationResponse evaluateTraffic(EvaluationRequest request);

    List<EvaluationHistoryEntry> getEvaluationHistory();
}
