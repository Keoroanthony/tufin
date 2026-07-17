package com.tufin.policyengine.domain;

import java.time.Instant;
import java.util.Objects;

public final class Rule {

    private final String id;
    private final String name;
    private final int priority;
    private final String resource;
    private final String action;
    private final String subject;
    private final Decision decision;
    private final String description;
    private final Instant createdAt;

    public Rule(
            String id,
            String name,
            int priority,
            String resource,
            String action,
            String subject,
            Decision decision,
            String description,
            Instant createdAt) {

        requireNotBlank(id, "id");
        requireNotBlank(name, "name");
        requireNotBlank(resource, "resource");
        requireNotBlank(action, "action");
        requireNotBlank(subject, "subject");
        requireNotNull(decision, "decision");
        requireNotNull(createdAt, "createdAt");

        if (priority <= 0) {
            throw new IllegalArgumentException("Priority must be greater than zero");
        }
        if (!resource.startsWith("/")) {
            throw new IllegalArgumentException("Resource pattern must start with '/'");
        }

        this.id = id;
        this.name = name;
        this.priority = priority;
        this.resource = resource;
        this.action = action;
        this.subject = subject;
        this.decision = decision;
        this.description = description;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPriority() {
        return priority;
    }

    public String getResource() {
        return resource;
    }

    public String getAction() {
        return action;
    }

    public String getSubject() {
        return subject;
    }

    public Decision getDecision() {
        return decision;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rule other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Rule{id='" + id + "', name='" + name + "', priority=" + priority
                + ", resource='" + resource + "', action='" + action
                + "', subject='" + subject + "', decision=" + decision + "}";
    }

    private static void requireNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }

    private static void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
