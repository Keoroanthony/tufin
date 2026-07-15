package com.tufin.policyengine.history;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CircularBuffer<T> {

    private final Object[] buffer;
    private final int capacity;
    private int head = 0;
    private int tail = 0;
    private int size = 0;

    public CircularBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero");
        }
        this.capacity = capacity;
        this.buffer = new Object[capacity];
    }

    public synchronized void add(T item) {
        buffer[tail] = item;
        tail = (tail + 1) % capacity;
        if (size < capacity) {
            size++;
        } else {
            head = (head + 1) % capacity;
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized List<T> getAll() {
        List<T> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add((T) buffer[(head + i) % capacity]);
        }
        return Collections.unmodifiableList(result);
    }

    public synchronized int size() {
        return size;
    }
}
