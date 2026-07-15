package com.tufin.policyengine.strategy;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class WildcardResourceMatchingStrategy implements ResourceMatchingStrategy {

    @Override
    public boolean matches(String ruleResourcePattern, String requestedResource) {
        if (!ruleResourcePattern.contains("*")) {
            return ruleResourcePattern.equals(requestedResource);
        }
        String regex = toRegex(ruleResourcePattern);
        return requestedResource.matches(regex);
    }

    private String toRegex(String pattern) {
        return "^" + Arrays.stream(pattern.split("\\*", -1))
                .map(Pattern::quote)
                .collect(Collectors.joining(".*")) + "$";
    }
}
