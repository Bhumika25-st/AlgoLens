package com.algolens.algorithm.sorting;

import com.algolens.algorithm.ExecutionCancelledException;
import com.algolens.model.ElementState;
import com.algolens.model.StepSnapshot;
import java.util.Arrays;
import java.util.Objects;

/**
 * Mediates step-by-step instrumentation between sorting algorithms and the execution controller.
 * Tracks comparison/swap counts, persistent sorted/pivot flags, and emits immutable snapshots.
 */
public class SortingStepContext {

    /**
     * Callback invoked for every generated step snapshot.
     */
    @FunctionalInterface
    public interface StepEmitter {
        void onStep(StepSnapshot snapshot) throws ExecutionCancelledException;
    }

    private final boolean[] sortedFlags;
    private final StepEmitter emitter;
    private int activePivot = -1;
    private long comparisons;
    private long swaps;
    private long stepNumber;

    public SortingStepContext(int length, StepEmitter emitter) {
        if (length < 0) {
            throw new IllegalArgumentException("length must be non-negative");
        }
        this.sortedFlags = new boolean[length];
        this.emitter = Objects.requireNonNull(emitter, "emitter must not be null");
    }

    public int compare(int[] array, int i, int j, int activeLine, String message) {
        comparisons++;
        emitSnapshot(array, activeLine, message, new int[] {i, j}, new int[0]);
        return Integer.compare(array[i], array[j]);
    }

    public void swap(int[] array, int i, int j, int activeLine, String message) {
        if (i != j) {
            int temp = array[i];
            array[i] = array[j];
            array[j] = temp;
            swaps++;
        }
        emitSnapshot(array, activeLine, message, new int[0], new int[] {i, j});
    }

    public void markPivot(int[] array, int pivotIndex, int activeLine, String message) {
        this.activePivot = pivotIndex;
        emitSnapshot(array, activeLine, message, new int[0], new int[0]);
    }

    public void clearPivot() {
        this.activePivot = -1;
    }

    public void markSorted(int[] array, int index, int activeLine, String message) {
        if (index >= 0 && index < sortedFlags.length) {
            sortedFlags[index] = true;
        }
        emitSnapshot(array, activeLine, message, new int[0], new int[0]);
    }

    public void markRangeSorted(int[] array, int fromInclusive, int toInclusive, int activeLine, String message) {
        int start = Math.max(0, fromInclusive);
        int end = Math.min(sortedFlags.length - 1, toInclusive);
        for (int idx = start; idx <= end; idx++) {
            sortedFlags[idx] = true;
        }
        emitSnapshot(array, activeLine, message, new int[0], new int[0]);
    }

    public void highlightLine(int[] array, int activeLine, String message) {
        emitSnapshot(array, activeLine, message, new int[0], new int[0]);
    }

    public void markAllSorted(int[] array, int activeLine, String message) {
        this.activePivot = -1;
        Arrays.fill(sortedFlags, true);
        emitSnapshot(array, activeLine, message, new int[0], new int[0]);
    }

    public long getComparisons() {
        return comparisons;
    }

    public long getSwaps() {
        return swaps;
    }

    public long getStepNumber() {
        return stepNumber;
    }

    private void emitSnapshot(
            int[] array,
            int activeLine,
            String message,
            int[] comparingIndices,
            int[] swappingIndices) {
        stepNumber++;
        ElementState[] states = buildElementStates(array.length, comparingIndices, swappingIndices);
        StepSnapshot snapshot = new StepSnapshot(
                array,
                states,
                activeLine,
                message,
                comparisons,
                swaps,
                stepNumber);
        emitter.onStep(snapshot);
    }

    private ElementState[] buildElementStates(int length, int[] comparingIndices, int[] swappingIndices) {
        ElementState[] states = new ElementState[length];
        Arrays.fill(states, ElementState.DEFAULT);

        for (int i = 0; i < length && i < sortedFlags.length; i++) {
            if (sortedFlags[i]) {
                states[i] = ElementState.SORTED;
            }
        }

        if (activePivot >= 0 && activePivot < length) {
            states[activePivot] = ElementState.PIVOT;
        }

        for (int idx : comparingIndices) {
            if (idx >= 0 && idx < length && idx != activePivot) {
                states[idx] = ElementState.COMPARING;
            }
        }

        for (int idx : swappingIndices) {
            if (idx >= 0 && idx < length) {
                states[idx] = ElementState.SWAPPING;
            }
        }

        return states;
    }
}
