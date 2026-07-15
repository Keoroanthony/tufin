package com.tufin.policyengine.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WildcardResourceMatchingStrategyTest {

    private WildcardResourceMatchingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new WildcardResourceMatchingStrategy();
    }

    @Test
    void shouldMatchExactResource() {
        assertThat(strategy.matches("/admin/users", "/admin/users")).isTrue();
    }

    @Test
    void shouldNotMatchDifferentExactResource() {
        assertThat(strategy.matches("/admin/users", "/admin/settings")).isFalse();
    }

    @Test
    void shouldMatchWildcardSingleSegment() {
        assertThat(strategy.matches("/admin/*", "/admin/users")).isTrue();
        assertThat(strategy.matches("/admin/*", "/admin/settings")).isTrue();
    }

    @Test
    void shouldMatchWildcardAcrossMultipleSegments() {
        assertThat(strategy.matches("/admin/*", "/admin/profile/edit")).isTrue();
    }

    @Test
    void shouldNotMatchWildcardForDifferentPrefix() {
        assertThat(strategy.matches("/admin/*", "/user/profile")).isFalse();
    }

    @Test
    void shouldMatchRootWildcard() {
        assertThat(strategy.matches("/*", "/anything")).isTrue();
        assertThat(strategy.matches("/*", "/a/b/c")).isTrue();
    }

    @Test
    void shouldNotMatchEmptySegmentBeforeWildcard() {
        assertThat(strategy.matches("/admin/*", "/admin/")).isTrue();
        assertThat(strategy.matches("/admin/*", "/admin")).isFalse();
    }

    @Test
    void shouldMatchMidPathWildcard() {
        assertThat(strategy.matches("/api/*/resource", "/api/v1/resource")).isTrue();
        assertThat(strategy.matches("/api/*/resource", "/api/v2/resource")).isTrue();
        assertThat(strategy.matches("/api/*/resource", "/api/v1/other")).isFalse();
    }

    @Test
    void shouldMatchDefaultRuleStrategyWithWildcard() {
        ResourceMatchingStrategy resourceStrategy = new WildcardResourceMatchingStrategy();
        RuleMatchingStrategy ruleStrategy = new DefaultRuleMatchingStrategy(resourceStrategy);

        com.tufin.policyengine.domain.Rule rule = new com.tufin.policyengine.domain.Rule(
                "id-1", "Allow Admins", 100, "/admin/*", "READ", "ADMIN",
                com.tufin.policyengine.domain.Decision.ALLOW, null,
                java.time.Instant.parse("2026-07-14T10:00:00Z"));

        com.tufin.policyengine.dto.EvaluationRequest request =
                new com.tufin.policyengine.dto.EvaluationRequest("ADMIN", "/admin/users", "READ");

        assertThat(ruleStrategy.matches(rule, request)).isTrue();
    }

    @Test
    void shouldNotMatchWhenSubjectDiffersInStrategy() {
        ResourceMatchingStrategy resourceStrategy = new WildcardResourceMatchingStrategy();
        RuleMatchingStrategy ruleStrategy = new DefaultRuleMatchingStrategy(resourceStrategy);

        com.tufin.policyengine.domain.Rule rule = new com.tufin.policyengine.domain.Rule(
                "id-1", "Allow Admins", 100, "/admin/*", "READ", "ADMIN",
                com.tufin.policyengine.domain.Decision.ALLOW, null,
                java.time.Instant.parse("2026-07-14T10:00:00Z"));

        com.tufin.policyengine.dto.EvaluationRequest request =
                new com.tufin.policyengine.dto.EvaluationRequest("GUEST", "/admin/users", "READ");

        assertThat(ruleStrategy.matches(rule, request)).isFalse();
    }
}
