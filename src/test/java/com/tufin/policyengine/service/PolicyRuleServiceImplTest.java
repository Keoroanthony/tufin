package com.tufin.policyengine.service;

import com.tufin.policyengine.domain.Decision;
import com.tufin.policyengine.domain.Rule;
import com.tufin.policyengine.dto.EvaluationHistoryEntry;
import com.tufin.policyengine.dto.EvaluationRequest;
import com.tufin.policyengine.dto.EvaluationResponse;
import com.tufin.policyengine.history.EvaluationHistoryStore;
import com.tufin.policyengine.mapper.EvaluationMapper;
import com.tufin.policyengine.repository.RuleRepository;
import com.tufin.policyengine.service.impl.PolicyRuleServiceImpl;
import com.tufin.policyengine.strategy.DefaultRuleMatchingStrategy;
import com.tufin.policyengine.strategy.RuleMatchingStrategy;
import com.tufin.policyengine.strategy.WildcardResourceMatchingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyRuleServiceImplTest {

    private static final Instant T0 = Instant.parse("2026-07-14T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-07-14T10:01:00Z");
    private static final Instant T2 = Instant.parse("2026-07-14T10:02:00Z");

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private EvaluationHistoryStore historyStore;

    private PolicyRuleService service;

    @BeforeEach
    void setUp() {
        RuleMatchingStrategy strategy = new DefaultRuleMatchingStrategy(new WildcardResourceMatchingStrategy());
        EvaluationMapper mapper = new EvaluationMapper();
        service = new PolicyRuleServiceImpl(ruleRepository, strategy, mapper, historyStore);
    }

    private Rule allowAdminRule() {
        return new Rule("rule-001", "Allow Admins", 100, "/admin/*", "READ", "ADMIN",
                Decision.ALLOW, null, T0);
    }

    private Rule denyGuestRule() {
        return new Rule("rule-002", "Deny Guests", 50, "/admin/*", "READ", "GUEST",
                Decision.DENY, null, T1);
    }

    @Test
    void shouldReturnAllowWhenRuleMatches() {
        when(ruleRepository.findAll()).thenReturn(List.of(allowAdminRule()));

        EvaluationResponse response = service.evaluateTraffic(
                new EvaluationRequest("ADMIN", "/admin/users", "READ"));

        assertThat(response.decision()).isEqualTo("ALLOW");
        assertThat(response.matchedRuleId()).isEqualTo("rule-001");
        assertThat(response.matchedRuleName()).isEqualTo("Allow Admins");
    }

    @Test
    void shouldReturnDenyWhenDenyRuleMatches() {
        when(ruleRepository.findAll()).thenReturn(List.of(denyGuestRule()));

        EvaluationResponse response = service.evaluateTraffic(
                new EvaluationRequest("GUEST", "/admin/users", "READ"));

        assertThat(response.decision()).isEqualTo("DENY");
        assertThat(response.matchedRuleId()).isEqualTo("rule-002");
    }

    @Test
    void shouldReturnDefaultDenyWhenNoRuleMatches() {
        when(ruleRepository.findAll()).thenReturn(List.of(allowAdminRule()));

        EvaluationResponse response = service.evaluateTraffic(
                new EvaluationRequest("GUEST", "/admin/users", "READ"));

        assertThat(response.decision()).isEqualTo("DENY");
        assertThat(response.matchedRuleId()).isNull();
        assertThat(response.matchedRuleName()).isNull();
    }

    @Test
    void shouldMatchWildcardResource() {
        when(ruleRepository.findAll()).thenReturn(List.of(allowAdminRule()));

        assertThat(service.evaluateTraffic(
                new EvaluationRequest("ADMIN", "/admin/profile/edit", "READ")).decision())
                .isEqualTo("ALLOW");
    }

    @Test
    void shouldNotMatchWildcardForDifferentPrefix() {
        when(ruleRepository.findAll()).thenReturn(List.of(allowAdminRule()));

        EvaluationResponse response = service.evaluateTraffic(
                new EvaluationRequest("ADMIN", "/user/profile", "READ"));

        assertThat(response.decision()).isEqualTo("DENY");
        assertThat(response.matchedRuleId()).isNull();
    }

    @Test
    void shouldSelectHighestPriorityRule() {
        Rule lowPriority = new Rule("rule-low", "Low Priority", 10, "/admin/*", "READ", "ADMIN",
                Decision.DENY, null, T0);
        Rule highPriority = new Rule("rule-high", "High Priority", 100, "/admin/*", "READ", "ADMIN",
                Decision.ALLOW, null, T1);
        when(ruleRepository.findAll()).thenReturn(List.of(lowPriority, highPriority));

        EvaluationResponse response = service.evaluateTraffic(
                new EvaluationRequest("ADMIN", "/admin/users", "READ"));

        assertThat(response.decision()).isEqualTo("ALLOW");
        assertThat(response.matchedRuleId()).isEqualTo("rule-high");
    }

    @Test
    void shouldSelectOldestRuleWhenPrioritiesAreEqual() {
        Rule older = new Rule("rule-older", "Older Rule", 50, "/admin/*", "READ", "ADMIN",
                Decision.ALLOW, null, T0);
        Rule newer = new Rule("rule-newer", "Newer Rule", 50, "/admin/*", "READ", "ADMIN",
                Decision.DENY, null, T2);
        when(ruleRepository.findAll()).thenReturn(List.of(newer, older));

        EvaluationResponse response = service.evaluateTraffic(
                new EvaluationRequest("ADMIN", "/admin/users", "READ"));

        assertThat(response.matchedRuleId()).isEqualTo("rule-older");
        assertThat(response.decision()).isEqualTo("ALLOW");
    }

    @Test
    void shouldRecordEvaluationInHistory() {
        when(ruleRepository.findAll()).thenReturn(List.of(allowAdminRule()));

        service.evaluateTraffic(new EvaluationRequest("ADMIN", "/admin/users", "READ"));

        verify(historyStore).add(any(EvaluationHistoryEntry.class));
    }

    @Test
    void shouldDelegateGetHistoryToStore() {
        EvaluationHistoryEntry entry = new EvaluationHistoryEntry(
                T0, "ADMIN", "/admin/users", "READ", "ALLOW", "rule-001");
        when(historyStore.getAll()).thenReturn(List.of(entry));

        List<EvaluationHistoryEntry> history = service.getEvaluationHistory();

        assertThat(history).hasSize(1);
        assertThat(history.get(0).decision()).isEqualTo("ALLOW");
    }
}
