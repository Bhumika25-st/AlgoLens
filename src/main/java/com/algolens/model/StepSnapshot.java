package com.algolens.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable snapshot representing a single visualizable step of algorithm execution.
 * Defensive copies guarantee thread-safe handoff from background worker threads to the Swing EDT.
 */
public final class StepSnapshot {

    private final int[] values;
    private final ElementState[] states;
    private final int activeLine;
    private final String statusMessage;
    private final long comparisons;
    private final long swaps;
    private final long stepNumber;
    private final int lowIndex;
    private final int highIndex;
    private final int midIndex;
    private final int foundIndex;

    public StepSnapshot(
            int[] values,
            ElementState[] states,
            int activeLine,
            String statusMessage,
            long comparisons,
            long swaps,
            long stepNumber) {
        this(values, states, activeLine, statusMessage, comparisons, swaps, stepNumber, -1, -1, -1, -1);
    }

    public StepSnapshot(
            int[] values,
            ElementState[] states,
            int activeLine,
            String statusMessage,
            long comparisons,
            long swaps,
            long stepNumber,
            int lowIndex,
            int highIndex,
            int midIndex,
            int foundIndex) {
        Objects.requireNonNull(values, "values must not be null");
        Objects.requireNonNull(states, "states must not be null");
        if (values.length != states.length) {
            throw new IllegalArgumentException(
                    "values length (" + values.length + ") must match states length (" + states.length + ")");
        }
        this.values = Arrays.copyOf(values, values.length);
        this.states = Arrays.copyOf(states, states.length);
        this.activeLine = activeLine;
        this.statusMessage = statusMessage != null ? statusMessage : "";
        this.comparisons = comparisons;
        this.swaps = swaps;
        this.stepNumber = stepNumber;
        this.lowIndex = lowIndex;
        this.highIndex = highIndex;
        this.midIndex = midIndex;
        this.foundIndex = foundIndex;
    }

    public int[] getValues() {
        return Arrays.copyOf(values, values.length);
    }

    public ElementState[] getStates() {
        return Arrays.copyOf(states, states.length);
    }

    public int getValueAt(int index) {
        return values[index];
    }

    public ElementState getStateAt(int index) {
        return states[index];
    }

    public int getLength() {
        return values.length;
    }

    public int getActiveLine() {
        return activeLine;
    }

    public String getStatusMessage() {
        return statusMessage;
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

    public int getLowIndex() {
        return lowIndex;
    }

    public int getHighIndex() {
        return highIndex;
    }

    public int getMidIndex() {
        return midIndex;
    }

    public int getFoundIndex() {
        return foundIndex;
    }
}
