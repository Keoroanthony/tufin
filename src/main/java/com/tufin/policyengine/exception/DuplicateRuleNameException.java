package com.tufin.policyengine.exception;

public class DuplicateRuleNameException extends RuntimeException {

    public DuplicateRuleNameException(String name) {
        super("A rule with the same name already exists: " + name);
    }
}
