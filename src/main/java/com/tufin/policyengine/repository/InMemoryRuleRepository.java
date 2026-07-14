package com.tufin.policyengine.repository;

import com.tufin.policyengine.domain.Rule;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryRuleRepository implements RuleRepository {

    private final ConcurrentHashMap<String, Rule> store = new ConcurrentHashMap<>();

    @Override
    public Rule save(Rule rule) {
        store.put(rule.getId(), rule);
        return rule;
    }

    @Override
    public Optional<Rule> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Rule> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public boolean existsById(String id) {
        return store.containsKey(id);
    }

    @Override
    public boolean existsByName(String name) {
        return store.values().stream()
                .anyMatch(rule -> rule.getName().equals(name));
    }

    @Override
    public boolean deleteById(String id) {
        return store.remove(id) != null;
    }
}
