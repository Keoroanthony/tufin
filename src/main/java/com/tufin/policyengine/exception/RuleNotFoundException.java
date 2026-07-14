package com.tufin.policyengine.exception;

public class RuleNotFoundException extends RuntimeException {

    public RuleNotFoundException(String id) {
        super("Rule not found: " + id);
    }
}
