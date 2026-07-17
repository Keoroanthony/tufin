package com.tufin.policyengine.client;

import com.tufin.policyengine.dto.EvaluationRequest;
import com.tufin.policyengine.dto.EvaluationResponse;

public interface AuditServiceClient {

    void sendDenyAudit(EvaluationRequest request, EvaluationResponse response);
}
