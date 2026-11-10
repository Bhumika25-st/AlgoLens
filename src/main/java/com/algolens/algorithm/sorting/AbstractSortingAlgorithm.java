package com.algolens.algorithm.sorting;

import com.algolens.algorithm.AbstractAlgorithm;
import com.algolens.algorithm.AlgorithmMetadata;
import java.util.List;
import java.util.Objects;

/**
 * Base abstract class for extensible array-sorting algorithms in AlgoLens.
 * Subclasses implement {@link #doSort(int[], SortingStepContext)} using protected step helpers
 * to emit state transitions and synchronized source-line highlights.
 */
public abstract class AbstractSortingAlgorithm extends AbstractAlgorithm {

    protected AbstractSortingAlgorithm(AlgorithmMetadata metadata, List<String> codeLines) {
        super(metadata, codeLines);
    }

    /**
     * Template execution entry point ensuring non-null input handling and final sorted state emission.
     */
    public final void sort(int[] array, SortingStepContext context) {
        Objects.requireNonNull(array, "array must not be null");
        Objects.requireNonNull(context, "context must not be null");

        if (array.length <= 1) {
            context.markAllSorted(array, -1, "Array is trivially sorted.");
            return;
        }

        doSort(array, context);
        context.markAllSorted(
                array,
                -1,
                String.format(
                        "%s complete: %,d comparisons, %,d swaps.",
                        getDisplayName(),
                        context.getComparisons(),
                        context.getSwaps()));
    }

    protected abstract void doSort(int[] array, SortingStepContext context);

    protected int compare(SortingStepContext ctx, int[] array, int i, int j, int line, String message) {
        return ctx.compare(array, i, j, line, message);
    }

    protected void swap(SortingStepContext ctx, int[] array, int i, int j, int line, String message) {
        ctx.swap(array, i, j, line, message);
    }

    protected void markPivot(SortingStepContext ctx, int[] array, int index, int line, String message) {
        ctx.markPivot(array, index, line, message);
    }

    protected void clearPivot(SortingStepContext ctx) {
        ctx.clearPivot();
    }

    protected void markSorted(SortingStepContext ctx, int[] array, int index, int line, String message) {
        ctx.markSorted(array, index, line, message);
    }

    protected void markRangeSorted(
            SortingStepContext ctx, int[] array, int fromInclusive, int toInclusive, int line, String message) {
        ctx.markRangeSorted(array, fromInclusive, toInclusive, line, message);
    }

    protected void highlightLine(SortingStepContext ctx, int[] array, int line, String message) {
        ctx.highlightLine(array, line, message);
    }
}
