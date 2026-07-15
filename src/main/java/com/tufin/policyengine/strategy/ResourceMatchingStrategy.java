package com.tufin.policyengine.strategy;

public interface ResourceMatchingStrategy {

    boolean matches(String ruleResourcePattern, String requestedResource);
}
