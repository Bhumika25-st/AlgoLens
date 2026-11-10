package com.algolens.algorithm.searching;

import com.algolens.algorithm.AlgorithmCategory;
import com.algolens.algorithm.AlgorithmMetadata;
import com.algolens.model.ArrayModel;
import java.util.List;

/**
 * Binary Search implementation for sorted arrays with low/mid/high pointers and half-discarding visualization.
 */
public class BinarySearch extends AbstractSearchingAlgorithm {

    private static final AlgorithmMetadata METADATA = new AlgorithmMetadata(
            "binary-search",
            "Binary Search",
            AlgorithmCategory.SEARCHING,
            "O(1)",
            "O(log n)",
            "O(log n)",
            "O(1)",
            "Halves a sorted search interval repeatedly by comparing the midpoint element against the target.");

    private static final List<String> CODE_LINES = List.of(
            "int binarySearch(int[] arr, int target) {",
            "    int low = 0, high = arr.length - 1;",
            "    while (low <= high) {",
            "        int mid = low + (high - low) / 2;",
            "        if (arr[mid] == target) {",
            "            return mid;",
            "        } else if (arr[mid] < target) {",
            "            low = mid + 1;",
            "        } else {",
            "            high = mid - 1;",
            "        }",
            "    }",
            "    return -1;",
            "}");

    public BinarySearch() {
        super(METADATA, CODE_LINES);
    }

    @Override
    public boolean requiresSortedInput() {
        return true;
    }

    @Override
    protected int doSearch(int[] arr, int target, SearchStepContext ctx) {
        if (!ArrayModel.isSortedAscending(arr)) {
            throw new IllegalArgumentException("Binary Search requires a sorted array in ascending order.");
        }

        int low = 0;
        int high = arr.length - 1;
        setSearchRange(
                ctx,
                arr,
                low,
                high,
                2,
                String.format("Initialized search bounds [%d .. %d] for target %d", low, high, target));

        while (low <= high) {
            int mid = low + (high - low) / 2;
            int cmp = compareMid(
                    ctx,
                    arr,
                    low,
                    high,
                    mid,
                    5,
                    String.format(
                            "Checking mid index %d: arr[%d] (%d) vs target (%d)",
                            mid, mid, arr[mid], target));

            if (cmp == 0) {
                markFound(
                        ctx,
                        arr,
                        mid,
                        6,
                        String.format(
                                "Target %d found at index %d (%d comparisons).",
                                target, mid, ctx.getComparisons()));
                return mid;
            } else if (cmp < 0) {
                int oldLow = low;
                low = mid + 1;
                markRangeDiscarded(
                        ctx,
                        arr,
                        oldLow,
                        mid,
                        low,
                        high,
                        8,
                        String.format(
                                "arr[%d] (%d) < %d — discarding left half [%d .. %d]",
                                mid, arr[mid], target, oldLow, mid));
            } else {
                int oldHigh = high;
                high = mid - 1;
                markRangeDiscarded(
                        ctx,
                        arr,
                        mid,
                        oldHigh,
                        low,
                        high,
                        10,
                        String.format(
                                "arr[%d] (%d) > %d — discarding right half [%d .. %d]",
                                mid, arr[mid], target, mid, oldHigh));
            }
        }

        markNotFound(
                ctx,
                arr,
                13,
                String.format("Target %d not found in array (%d comparisons).", target, ctx.getComparisons()));
        return -1;
    }
}
