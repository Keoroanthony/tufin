package com.tufin.policyengine.config;

import com.tufin.policyengine.domain.Decision;
import com.tufin.policyengine.domain.Rule;
import com.tufin.policyengine.repository.RuleRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class RuleDataInitializer implements ApplicationRunner {

    private final RuleRepository ruleRepository;

    public RuleDataInitializer(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        ruleRepository.save(new Rule(
                "rule-001", "Allow Admins", 100, "/admin/*", "READ", "ADMIN",
                Decision.ALLOW, "Allows administrators to read admin resources",
                Instant.parse("2026-07-14T10:00:00Z")));

        ruleRepository.save(new Rule(
                "rule-002", "Deny Guests Admin", 50, "/admin/*", "READ", "GUEST",
                Decision.DENY, "Denies guest access to admin resources",
                Instant.parse("2026-07-14T10:01:00Z")));

        ruleRepository.save(new Rule(
                "rule-003", "Allow Users Profile", 80, "/user/*", "READ", "USER",
                Decision.ALLOW, "Allows users to read their own profile",
                Instant.parse("2026-07-14T10:02:00Z")));
    }
}
