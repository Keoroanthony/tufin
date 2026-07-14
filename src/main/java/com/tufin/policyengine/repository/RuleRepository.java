package com.tufin.policyengine.repository;

import com.tufin.policyengine.domain.Rule;

import java.util.List;
import java.util.Optional;

public interface RuleRepository {

    Rule save(Rule rule);

    Optional<Rule> findById(String id);

    List<Rule> findAll();

    boolean existsById(String id);

    boolean existsByName(String name);

    boolean deleteById(String id);
}
