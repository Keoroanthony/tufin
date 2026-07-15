package com.tufin.policyengine.history;

import com.tufin.policyengine.dto.EvaluationHistoryEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EvaluationHistoryStore {

    private final CircularBuffer<EvaluationHistoryEntry> buffer;

    public EvaluationHistoryStore(@Value("${evaluation.history.capacity:100}") int capacity) {
        this.buffer = new CircularBuffer<>(capacity);
    }

    public void add(EvaluationHistoryEntry entry) {
        buffer.add(entry);
    }

    public List<EvaluationHistoryEntry> getAll() {
        return buffer.getAll();
    }
}
