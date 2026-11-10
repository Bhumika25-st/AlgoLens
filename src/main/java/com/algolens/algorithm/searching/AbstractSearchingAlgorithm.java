package com.algolens.algorithm.searching;

import com.algolens.algorithm.AbstractAlgorithm;
import com.algolens.algorithm.AlgorithmMetadata;
import java.util.List;
import java.util.Objects;

/**
 * Base abstract class for extensible array-searching algorithms in AlgoLens.
 * Subclasses implement {@link #doSearch(int[], int, SearchStepContext)} using protected step
 * helpers to emit comparisons, discarded search ranges, and synchronized code line highlights.
 */
public abstract class AbstractSearchingAlgorithm extends AbstractAlgorithm {

    protected AbstractSearchingAlgorithm(AlgorithmMetadata metadata, List<String> codeLines) {
        super(metadata, codeLines);
    }

    /**
     * Executes the searching algorithm on the given array for {@code target}.
     *
     * @return index of target if found, or {@code -1} if absent
     */
    public final int search(int[] array, int target, SearchStepContext context) {
        Objects.requireNonNull(array, "array must not be null");
        Objects.requireNonNull(context, "context must not be null");

        if (array.length == 0) {
            context.markNotFound(array, -1, "Array is empty.");
            return -1;
        }

        return doSearch(array, target, context);
    }

    /**
     * Indicates whether the algorithm requires an ascending-sorted input array.
     */
    public boolean requiresSortedInput() {
        return false;
    }

    protected abstract int doSearch(int[] array, int target, SearchStepContext context);

    protected void setSearchRange(
            SearchStepContext ctx, int[] array, int low, int high, int line, String message) {
        ctx.setSearchRange(array, low, high, line, message);
    }

    protected int compareAt(SearchStepContext ctx, int[] array, int index, int line, String message) {
        return ctx.compareAt(array, index, line, message);
    }

    protected int compareMid(
            SearchStepContext ctx, int[] array, int low, int high, int mid, int line, String message) {
        return ctx.compareMid(array, low, high, mid, line, message);
    }

    protected void markDiscarded(SearchStepContext ctx, int[] array, int index, int line, String message) {
        ctx.markDiscarded(array, index, line, message);
    }

    protected void markRangeDiscarded(
            SearchStepContext ctx,
            int[] array,
            int fromInclusive,
            int toInclusive,
            int newLow,
            int newHigh,
            int line,
            String message) {
        ctx.markRangeDiscarded(array, fromInclusive, toInclusive, newLow, newHigh, line, message);
    }

    protected void markFound(SearchStepContext ctx, int[] array, int index, int line, String message) {
        ctx.markFound(array, index, line, message);
    }

    protected void markNotFound(SearchStepContext ctx, int[] array, int line, String message) {
        ctx.markNotFound(array, line, message);
    }

    protected void highlightLine(SearchStepContext ctx, int[] array, int line, String message) {
        ctx.highlightLine(array, line, message);
    }
}
