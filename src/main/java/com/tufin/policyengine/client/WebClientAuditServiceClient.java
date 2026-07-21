package com.tufin.policyengine.client;

import com.tufin.policyengine.dto.AuditRequest;
import com.tufin.policyengine.dto.EvaluationRequest;
import com.tufin.policyengine.dto.EvaluationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class WebClientAuditServiceClient implements AuditServiceClient {

    private static final Logger log = LoggerFactory.getLogger(WebClientAuditServiceClient.class);

    private final WebClient webClient;

    public WebClientAuditServiceClient(WebClient notificationWebClient) {
        this.webClient = notificationWebClient;
    }

    @Override
    public void sendDenyAudit(EvaluationRequest request, EvaluationResponse response) {
        String reason = response.matchedRuleName() != null
                ? "Rule '" + response.matchedRuleName() + "' matched"
                : "No matching rule — default deny";

        AuditRequest auditRequest = new AuditRequest(
                request.subject(),
                request.resource(),
                request.action(),
                response.decision(),
                response.matchedRuleId(),
                reason);

        webClient.post()
                .uri("/api/v1/notifications")
                .bodyValue(auditRequest)
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        result -> log.debug("Notification sent: subject={} resource={} reason={}",
                                request.subject(), request.resource(), reason),
                        error -> {
                            if (error instanceof WebClientResponseException ex) {
                                log.error("Notification rejected (HTTP {}): subject={} resource={} — check field contract",
                                        ex.getStatusCode().value(), request.subject(), request.resource());
                            } else {
                                log.warn("Notification delivery failed (service unavailable?): subject={} resource={}: {}",
                                        request.subject(), request.resource(), error.getMessage());
                            }
                        }
                );
    }
}
