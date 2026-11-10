package com.algolens.algorithm.searching;

import com.algolens.algorithm.ExecutionCancelledException;
import com.algolens.model.ElementState;
import com.algolens.model.StepSnapshot;
import java.util.Arrays;
import java.util.Objects;

/**
 * Mediates step-by-step instrumentation between searching algorithms and the execution controller.
 * Tracks comparison counts, search bounds (low/high/mid), discarded indices, and emits immutable snapshots.
 */
public class SearchStepContext {

    /**
     * Callback invoked for every generated step snapshot.
     */
    @FunctionalInterface
    public interface StepEmitter {
        void onStep(StepSnapshot snapshot) throws ExecutionCancelledException;
    }

    private final boolean[] discardedFlags;
    private final int target;
    private final StepEmitter emitter;
    private int lowIndex = -1;
    private int highIndex = -1;
    private int midIndex = -1;
    private int foundIndex = -1;
    private long comparisons;
    private long stepNumber;

    public SearchStepContext(int length, int target, StepEmitter emitter) {
        if (length < 0) {
            throw new IllegalArgumentException("length must be non-negative");
        }
        this.discardedFlags = new boolean[length];
        this.target = target;
        this.emitter = Objects.requireNonNull(emitter, "emitter must not be null");
    }

    public int getTarget() {
        return target;
    }

    public void setSearchRange(int[] array, int low, int high, int activeLine, String message) {
        this.lowIndex = low;
        this.highIndex = high;
        this.midIndex = -1;
        emitSnapshot(array, activeLine, message, -1);
    }

    public int compareAt(int[] array, int index, int activeLine, String message) {
        comparisons++;
        emitSnapshot(array, activeLine, message, index);
        return Integer.compare(array[index], target);
    }

    public int compareMid(int[] array, int low, int high, int mid, int activeLine, String message) {
        this.lowIndex = low;
        this.highIndex = high;
        this.midIndex = mid;
        comparisons++;
        emitSnapshot(array, activeLine, message, mid);
        return Integer.compare(array[mid], target);
    }

    public void markDiscarded(int[] array, int index, int activeLine, String message) {
        if (index >= 0 && index < discardedFlags.length) {
            discardedFlags[index] = true;
        }
        emitSnapshot(array, activeLine, message, -1);
    }

    public void markRangeDiscarded(
            int[] array,
            int fromInclusive,
            int toInclusive,
            int newLow,
            int newHigh,
            int activeLine,
            String message) {
        int start = Math.max(0, fromInclusive);
        int end = Math.min(discardedFlags.length - 1, toInclusive);
        for (int idx = start; idx <= end; idx++) {
            discardedFlags[idx] = true;
        }
        this.lowIndex = newLow;
        this.highIndex = newHigh;
        this.midIndex = -1;
        emitSnapshot(array, activeLine, message, -1);
    }

    public void markFound(int[] array, int index, int activeLine, String message) {
        this.foundIndex = index;
        emitSnapshot(array, activeLine, message, -1);
    }

    public void markNotFound(int[] array, int activeLine, String message) {
        Arrays.fill(discardedFlags, true);
        this.lowIndex = -1;
        this.highIndex = -1;
        this.midIndex = -1;
        this.foundIndex = -1;
        emitSnapshot(array, activeLine, message, -1);
    }

    public void highlightLine(int[] array, int activeLine, String message) {
        emitSnapshot(array, activeLine, message, -1);
    }

    public long getComparisons() {
        return comparisons;
    }

    public long getStepNumber() {
        return stepNumber;
    }

    private void emitSnapshot(int[] array, int activeLine, String message, int comparingIndex) {
        stepNumber++;
        ElementState[] states = buildElementStates(array.length, comparingIndex);
        StepSnapshot snapshot = new StepSnapshot(
                array,
                states,
                activeLine,
                message,
                comparisons,
                0L,
                stepNumber,
                lowIndex,
                highIndex,
                midIndex,
                foundIndex);
        emitter.onStep(snapshot);
    }

    private ElementState[] buildElementStates(int length, int comparingIndex) {
        ElementState[] states = new ElementState[length];
        Arrays.fill(states, ElementState.DEFAULT);

        for (int i = 0; i < length && i < discardedFlags.length; i++) {
            if (discardedFlags[i]) {
                states[i] = ElementState.DISCARDED;
            }
        }

        if (comparingIndex >= 0 && comparingIndex < length) {
            states[comparingIndex] = ElementState.COMPARING;
        }

        if (foundIndex >= 0 && foundIndex < length) {
            states[foundIndex] = ElementState.FOUND;
        }

        return states;
    }
}
