package com.algolens.model;

import com.algolens.algorithm.AlgorithmCategory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Central observable data model storing the array values, per-element visual states,
 * execution metrics, search target/range pointers, and the initial dataset for clean resets.
 */
public class ArrayModel {

    public static final int DEFAULT_SIZE = 32;
    public static final int MIN_ARRAY_SIZE = 2;
    public static final int MAX_ARRAY_SIZE = 100;
    public static final int MIN_VALUE = 10;
    public static final int MAX_VALUE = 100;

    /**
     * Observer notified on the Swing EDT when model state changes.
     */
    @FunctionalInterface
    public interface ModelListener {
        void onModelChanged(ArrayModel model);
    }

    private final List<ModelListener> listeners = new ArrayList<>();
    private final Random random = new Random();

    private int[] initialValues;
    private int[] values;
    private ElementState[] states;
    private int activeLine = -1;
    private String statusMessage = "Ready. Press Play or Step to visualize.";
    private long comparisons;
    private long swaps;
    private long stepNumber;
    private int lowIndex = -1;
    private int highIndex = -1;
    private int midIndex = -1;
    private int foundIndex = -1;
    private int searchTarget;
    private AlgorithmCategory activeCategory = AlgorithmCategory.SORTING;
    private ArrayPreset currentPreset = ArrayPreset.RANDOM;

    public ArrayModel() {
        generate(DEFAULT_SIZE, ArrayPreset.RANDOM);
        this.searchTarget = initialValues[initialValues.length / 2];
    }

    public void addListener(ModelListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        listeners.add(listener);
    }

    public void removeListener(ModelListener listener) {
        listeners.remove(listener);
    }

    /**
     * Generates a fresh dataset using the requested size and distribution preset.
     */
    public void generate(int size, ArrayPreset preset) {
        if (size < MIN_ARRAY_SIZE || size > MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException(
                    "Array size must be between " + MIN_ARRAY_SIZE + " and " + MAX_ARRAY_SIZE);
        }
        Objects.requireNonNull(preset, "preset must not be null");
        this.currentPreset = preset;

        int[] generated = new int[size];
        switch (preset) {
            case RANDOM -> {
                for (int i = 0; i < size; i++) {
                    generated[i] = MIN_VALUE + random.nextInt(MAX_VALUE - MIN_VALUE + 1);
                }
            }
            case NEARLY_SORTED -> {
                for (int i = 0; i < size; i++) {
                    generated[i] = MIN_VALUE + (int) Math.round(((double) i / (size - 1)) * (MAX_VALUE - MIN_VALUE));
                }
                int perturbations = Math.max(1, size / 6);
                for (int p = 0; p < perturbations; p++) {
                    int a = random.nextInt(size);
                    int b = Math.min(size - 1, Math.max(0, a + random.nextInt(5) - 2));
                    int tmp = generated[a];
                    generated[a] = generated[b];
                    generated[b] = tmp;
                }
            }
            case REVERSED -> {
                for (int i = 0; i < size; i++) {
                    generated[i] = MAX_VALUE - (int) Math.round(((double) i / (size - 1)) * (MAX_VALUE - MIN_VALUE));
                }
            }
            case FEW_UNIQUE -> {
                int[] buckets = {20, 40, 60, 80, 100};
                for (int i = 0; i < size; i++) {
                    generated[i] = buckets[random.nextInt(buckets.length)];
                }
            }
        }

        this.initialValues = Arrays.copyOf(generated, generated.length);
        restoreInitialState("Generated " + size + " elements (" + preset.getDisplayName() + ").");
    }

    /**
     * Parses a comma-separated string of integers into an array suitable for visualization.
     *
     * @throws IllegalArgumentException if the input is blank, malformed, or outside size bounds
     */
    public static int[] parseCustomValues(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            throw new IllegalArgumentException(
                    "Please enter comma-separated integers (e.g., 42, 12, 89, 5, 23).");
        }

        String[] tokens = rawInput.trim().split(",", -1);
        if (tokens.length < MIN_ARRAY_SIZE) {
            throw new IllegalArgumentException(
                    "Please enter at least " + MIN_ARRAY_SIZE + " comma-separated integers.");
        }
        if (tokens.length > MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException(
                    "Please enter at most " + MAX_ARRAY_SIZE + " integers.");
        }

        int[] parsed = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i].trim();
            if (token.isEmpty()) {
                throw new IllegalArgumentException(
                        "Empty value found near element " + (i + 1) + ". Remove extra commas.");
            }
            try {
                parsed[i] = Integer.parseInt(token);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(
                        "Invalid integer '" + token + "'. Use comma-separated whole numbers.");
            }
        }
        return parsed;
    }

    /**
     * Replaces the dataset with user-supplied custom values and resets execution metrics.
     */
    public void setCustomValues(int[] customValues) {
        Objects.requireNonNull(customValues, "customValues must not be null");
        if (customValues.length < MIN_ARRAY_SIZE || customValues.length > MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException(
                    "Custom array size must be between " + MIN_ARRAY_SIZE + " and " + MAX_ARRAY_SIZE);
        }
        this.initialValues = Arrays.copyOf(customValues, customValues.length);
        restoreInitialState("Loaded custom array (" + customValues.length + " elements).");
    }

    /**
     * Ensures the initial dataset is sorted ascending (required for algorithms such as Binary Search).
     */
    public void ensureSortedAscending(String algorithmName) {
        if (!isSortedAscending(initialValues)) {
            Arrays.sort(initialValues);
            restoreInitialState("Sorted dataset ascending for " + algorithmName + " (" + initialValues.length + " elements).");
        } else {
            restoreInitialState("Ready for " + algorithmName + " (" + initialValues.length + " elements).");
        }
    }

    public static boolean isSortedAscending(int[] array) {
        for (int i = 1; i < array.length; i++) {
            if (array[i - 1] > array[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean isInitialSorted() {
        return isSortedAscending(initialValues);
    }

    /**
     * Restores the array back to its initial dataset prior to execution.
     */
    public void resetToInitial() {
        restoreInitialState("Reset to initial array (" + values.length + " elements).");
    }

    private void restoreInitialState(String message) {
        this.values = Arrays.copyOf(initialValues, initialValues.length);
        this.states = new ElementState[values.length];
        Arrays.fill(this.states, ElementState.DEFAULT);
        this.activeLine = -1;
        this.comparisons = 0;
        this.swaps = 0;
        this.stepNumber = 0;
        this.lowIndex = -1;
        this.highIndex = -1;
        this.midIndex = -1;
        this.foundIndex = -1;
        this.statusMessage = message;
        notifyListeners();
    }

    /**
     * Applies an immutable execution snapshot emitted by the algorithm engine.
     */
    public void applySnapshot(StepSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        this.values = snapshot.getValues();
        this.states = snapshot.getStates();
        this.activeLine = snapshot.getActiveLine();
        this.statusMessage = snapshot.getStatusMessage();
        this.comparisons = snapshot.getComparisons();
        this.swaps = snapshot.getSwaps();
        this.stepNumber = snapshot.getStepNumber();
        this.lowIndex = snapshot.getLowIndex();
        this.highIndex = snapshot.getHighIndex();
        this.midIndex = snapshot.getMidIndex();
        this.foundIndex = snapshot.getFoundIndex();
        notifyListeners();
    }

    public void setActiveCategory(AlgorithmCategory activeCategory) {
        this.activeCategory = Objects.requireNonNull(activeCategory, "activeCategory must not be null");
        notifyListeners();
    }

    public AlgorithmCategory getActiveCategory() {
        return activeCategory;
    }

    public void setSearchTarget(int searchTarget) {
        this.searchTarget = searchTarget;
        notifyListeners();
    }

    public int getSearchTarget() {
        return searchTarget;
    }

    public int[] getInitialValuesCopy() {
        return Arrays.copyOf(initialValues, initialValues.length);
    }

    public int[] getValuesCopy() {
        return Arrays.copyOf(values, values.length);
    }

    public ElementState[] getStatesCopy() {
        return Arrays.copyOf(states, states.length);
    }

    public int getSize() {
        return values.length;
    }

    public int getValueAt(int index) {
        return values[index];
    }

    public ElementState getStateAt(int index) {
        return states[index];
    }

    public int getMinValue() {
        if (values.length == 0) {
            return 0;
        }
        int min = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] < min) {
                min = values[i];
            }
        }
        return min;
    }

    public int getMaxValue() {
        if (values.length == 0) {
            return MAX_VALUE;
        }
        int max = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] > max) {
                max = values[i];
            }
        }
        return max;
    }

    public int getMaxPossibleValue() {
        return Math.max(MAX_VALUE, getMaxValue());
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

    public ArrayPreset getCurrentPreset() {
        return currentPreset;
    }

    private void notifyListeners() {
        for (ModelListener listener : listeners) {
            listener.onModelChanged(this);
        }
    }
}
