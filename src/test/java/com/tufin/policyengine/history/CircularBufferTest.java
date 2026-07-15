package com.tufin.policyengine.history;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CircularBufferTest {

    @Test
    void shouldThrowWhenCapacityIsZero() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CircularBuffer<>(0))
                .withMessageContaining("Capacity");
    }

    @Test
    void shouldThrowWhenCapacityIsNegative() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CircularBuffer<>(-1))
                .withMessageContaining("Capacity");
    }

    @Test
    void shouldReturnEmptyListWhenNothingAdded() {
        CircularBuffer<String> buffer = new CircularBuffer<>(3);
        assertThat(buffer.getAll()).isEmpty();
        assertThat(buffer.size()).isZero();
    }

    @Test
    void shouldReturnItemsInInsertionOrder() {
        CircularBuffer<String> buffer = new CircularBuffer<>(5);
        buffer.add("a");
        buffer.add("b");
        buffer.add("c");

        assertThat(buffer.getAll()).containsExactly("a", "b", "c");
    }

    @Test
    void shouldNotExceedCapacity() {
        CircularBuffer<String> buffer = new CircularBuffer<>(3);
        buffer.add("a");
        buffer.add("b");
        buffer.add("c");

        assertThat(buffer.size()).isEqualTo(3);
        assertThat(buffer.getAll()).containsExactly("a", "b", "c");
    }

    @Test
    void shouldOverwriteOldestWhenFull() {
        CircularBuffer<String> buffer = new CircularBuffer<>(3);
        buffer.add("a");
        buffer.add("b");
        buffer.add("c");
        buffer.add("d");

        assertThat(buffer.size()).isEqualTo(3);
        assertThat(buffer.getAll()).containsExactly("b", "c", "d");
    }

    @Test
    void shouldReturnOldestToNewestAfterMultipleOverwrites() {
        CircularBuffer<Integer> buffer = new CircularBuffer<>(3);
        for (int i = 1; i <= 7; i++) {
            buffer.add(i);
        }

        assertThat(buffer.getAll()).containsExactly(5, 6, 7);
    }

    @Test
    void shouldHandleCapacityOfOne() {
        CircularBuffer<String> buffer = new CircularBuffer<>(1);
        buffer.add("first");
        buffer.add("second");

        assertThat(buffer.size()).isEqualTo(1);
        assertThat(buffer.getAll()).containsExactly("second");
    }

    @Test
    void shouldReturnUnmodifiableList() {
        CircularBuffer<String> buffer = new CircularBuffer<>(3);
        buffer.add("a");

        List<String> snapshot = buffer.getAll();

        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.add("mutate"));
    }
}
