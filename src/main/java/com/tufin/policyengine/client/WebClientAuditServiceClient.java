package com.tufin.policyengine.client;

import com.tufin.policyengine.dto.AuditRequest;
import com.tufin.policyengine.dto.EvaluationRequest;
import com.tufin.policyengine.dto.EvaluationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;

@Component
public class WebClientAuditServiceClient implements AuditServiceClient {

    private static final Logger log = LoggerFactory.getLogger(WebClientAuditServiceClient.class);

    private final WebClient webClient;

    public WebClientAuditServiceClient(WebClient auditWebClient) {
        this.webClient = auditWebClient;
    }

    @Override
    public void sendDenyAudit(EvaluationRequest request, EvaluationResponse response) {
        AuditRequest auditRequest = new AuditRequest(
                request.subject(),
                request.resource(),
                request.action(),
                response.decision(),
                response.matchedRuleId(),
                Instant.now());

        webClient.post()
                .uri("/audit/deny")
                .bodyValue(auditRequest)
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        result -> log.debug("Audit event sent for subject={} resource={}", request.subject(), request.resource()),
                        error -> log.warn("Audit call failed (subject={} resource={}): {}", request.subject(), request.resource(), error.getMessage())
                );
    }
}
