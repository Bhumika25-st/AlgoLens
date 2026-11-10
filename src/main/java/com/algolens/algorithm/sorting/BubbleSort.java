package com.algolens.algorithm.sorting;

import com.algolens.algorithm.AlgorithmCategory;
import com.algolens.algorithm.AlgorithmMetadata;
import java.util.List;

/**
 * Bubble Sort implementation with early-exit optimization and step-by-step instrumentation.
 */
public class BubbleSort extends AbstractSortingAlgorithm {

    private static final AlgorithmMetadata METADATA = new AlgorithmMetadata(
            "bubble-sort",
            "Bubble Sort",
            AlgorithmCategory.SORTING,
            "O(n)",
            "O(n^2)",
            "O(n^2)",
            "O(1)",
            "Repeatedly steps through the array, swapping adjacent out-of-order elements until sorted.");

    private static final List<String> CODE_LINES = List.of(
            "void bubbleSort(int[] arr) {",
            "    int n = arr.length;",
            "    for (int i = 0; i < n - 1; i++) {",
            "        boolean swapped = false;",
            "        for (int j = 0; j < n - i - 1; j++) {",
            "            if (arr[j] > arr[j + 1]) {",
            "                swap(arr, j, j + 1);",
            "                swapped = true;",
            "            }",
            "        }",
            "        markSorted(n - i - 1);",
            "        if (!swapped) break;",
            "    }",
            "    markSorted(0);",
            "}");

    public BubbleSort() {
        super(METADATA, CODE_LINES);
    }

    @Override
    protected void doSort(int[] arr, SortingStepContext ctx) {
        int n = arr.length;
        boolean completedEarly = false;
        for (int i = 0; i < n - 1; i++) {
            highlightLine(ctx, arr, 3, "Starting pass " + (i + 1) + " of " + (n - 1));
            boolean swapped = false;

            for (int j = 0; j < n - i - 1; j++) {
                int cmp = compare(
                        ctx,
                        arr,
                        j,
                        j + 1,
                        6,
                        String.format("Comparing arr[%d] (%d) > arr[%d] (%d)", j, arr[j], j + 1, arr[j + 1]));
                if (cmp > 0) {
                    swap(
                            ctx,
                            arr,
                            j,
                            j + 1,
                            7,
                            String.format("Swapping arr[%d] (%d) and arr[%d] (%d)", j, arr[j], j + 1, arr[j + 1]));
                    swapped = true;
                }
            }

            int sortedIdx = n - i - 1;
            markSorted(ctx, arr, sortedIdx, 11, "Element at index " + sortedIdx + " is in final position");

            if (!swapped) {
                markRangeSorted(
                        ctx,
                        arr,
                        0,
                        sortedIdx - 1,
                        12,
                        "No swaps in pass " + (i + 1) + " — remaining prefix is already sorted");
                completedEarly = true;
                break;
            }
        }
        if (!completedEarly) {
            markSorted(ctx, arr, 0, 14, "Index 0 confirmed sorted");
        }
    }
}
