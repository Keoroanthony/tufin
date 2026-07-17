package com.tufin.policyengine.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

class RuleTest {

    private static final Instant NOW = Instant.parse("2026-07-14T10:00:00Z");

    private Rule adminReadRule() {
        return new Rule("id-1", "Allow Admins", 100, "/admin/*", "READ", "ADMIN", Decision.ALLOW, "desc", NOW);
    }

    // ── Null / blank field validation ────────────────────────────────────────

    @Test
    void shouldThrowWhenIdIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Rule(null, "name", 1, "/res", "READ", "ADMIN", Decision.ALLOW, null, NOW))
                .withMessageContaining("id");
    }

    @Test
    void shouldThrowWhenIdIsBlank() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Rule("   ", "name", 1, "/res", "READ", "ADMIN", Decision.ALLOW, null, NOW))
                .withMessageContaining("id");
    }

    @Test
    void shouldThrowWhenNameIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Rule("id-1", null, 1, "/res", "READ", "ADMIN", Decision.ALLOW, null, NOW))
                .withMessageContaining("name");
    }

    @Test
    void shouldThrowWhenNameIsBlank() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Rule("id-1", "", 1, "/res", "READ", "ADMIN", Decision.ALLOW, null, NOW))
                .withMessageContaining("name");
    }

    @Test
    void shouldThrowWhenResourceIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Rule("id-1", "name", 1, null, "READ", "ADMIN", Decision.ALLOW, null, NOW))
                .withMessageContaining("resource");
    }

    @Test
    void shouldThrowWhenResourceIsBlank() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Rule("id-1", "name", 1, "  ", "READ", "ADMIN", Decision.ALLOW, null, NOW))
                .withMessageContaining("resource");
    }

    @Test
    void shouldThrowWhenActionIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Rule("id-1", "name", 1, "/res", null, "ADMIN", Decision.ALLOW, null, NOW))
                .withMessageContaining("action");
    }

    @Test
    void shouldThrowWhenActionIsBlank() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Rule("id-1", "name", 1, "/res", "", "ADMIN", Decision.ALLOW, null, NOW))
                .withMessageContaining("action");
    }

    @Test
    void shouldThrowWhenSubjectIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Rule("id-1", "name", 1, "/res", "READ", null, Decision.ALLOW, null, NOW))
                .withMessageContaining("subject");
    }

    @Test
    void shouldThrowWhenSubjectIsBlank() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Rule("id-1", "name", 1, "/res", "READ", "  ", Decision.ALLOW, null, NOW))
                .withMessageContaining("subject");
    }

    @Test
    void shouldThrowWhenDecisionIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Rule("id-1", "name", 1, "/res", "READ", "ADMIN", null, null, NOW))
                .withMessageContaining("decision");
    }

    @Test
    void shouldThrowWhenCreatedAtIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Rule("id-1", "name", 1, "/res", "READ", "ADMIN", Decision.ALLOW, null, null))
                .withMessageContaining("createdAt");
    }

    @Test
    void shouldAllowNullDescription() {
        assertThatNoException()
                .isThrownBy(() -> new Rule("id-1", "name", 1, "/res", "READ", "ADMIN", Decision.ALLOW, null, NOW));
    }

    // ── Priority boundary tests ───────────────────────────────────────────────

    @Test
    void shouldThrowWhenPriorityIsZero() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Rule("id-1", "name", 0, "/res", "READ", "ADMIN", Decision.ALLOW, null, NOW))
                .withMessageContaining("Priority");
    }

    @Test
    void shouldThrowWhenPriorityIsNegative() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Rule("id-1", "name", -100, "/res", "READ", "ADMIN", Decision.ALLOW, null, NOW))
                .withMessageContaining("Priority");
    }

    @Test
    void shouldAcceptMinimumPriorityOfOne() {
        assertThatNoException()
                .isThrownBy(() -> new Rule("id-1", "name", 1, "/res", "READ", "ADMIN", Decision.ALLOW, null, NOW));
    }

    @Test
    void shouldAcceptMaxIntPriority() {
        assertThatNoException()
                .isThrownBy(() -> new Rule("id-1", "name", Integer.MAX_VALUE, "/res", "READ", "ADMIN", Decision.ALLOW, null, NOW));
    }

    // ── Resource pattern validation ───────────────────────────────────────────

    @Test
    void shouldThrowWhenResourceDoesNotStartWithSlash() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Rule("id-1", "name", 1, "admin/users", "READ", "ADMIN", Decision.ALLOW, null, NOW))
                .withMessageContaining("'/'");
    }

    // ── Field accessors ───────────────────────────────────────────────────────

    @Test
    void shouldExposeAllFields() {
        Rule rule = new Rule("id-1", "Allow Admins", 100, "/admin/*", "READ", "ADMIN", Decision.ALLOW, "desc", NOW);

        assertThat(rule.getId()).isEqualTo("id-1");
        assertThat(rule.getName()).isEqualTo("Allow Admins");
        assertThat(rule.getPriority()).isEqualTo(100);
        assertThat(rule.getResource()).isEqualTo("/admin/*");
        assertThat(rule.getAction()).isEqualTo("READ");
        assertThat(rule.getSubject()).isEqualTo("ADMIN");
        assertThat(rule.getDecision()).isEqualTo(Decision.ALLOW);
        assertThat(rule.getDescription()).isEqualTo("desc");
        assertThat(rule.getCreatedAt()).isEqualTo(NOW);
    }

    // ── Equality based on ID ──────────────────────────────────────────────────

    @Test
    void shouldConsiderRulesWithSameIdEqual() {
        Rule a = new Rule("id-1", "Rule A", 10, "/res", "READ", "ADMIN", Decision.ALLOW, null, NOW);
        Rule b = new Rule("id-1", "Rule B", 50, "/other", "WRITE", "GUEST", Decision.DENY, null, NOW);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void shouldConsiderRulesWithDifferentIdsNotEqual() {
        Rule a = new Rule("id-1", "name", 10, "/res", "READ", "ADMIN", Decision.ALLOW, null, NOW);
        Rule b = new Rule("id-2", "name", 10, "/res", "READ", "ADMIN", Decision.ALLOW, null, NOW);
        assertThat(a).isNotEqualTo(b);
    }
}
