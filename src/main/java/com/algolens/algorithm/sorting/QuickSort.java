package com.algolens.algorithm.sorting;

import com.algolens.algorithm.AlgorithmCategory;
import com.algolens.algorithm.AlgorithmMetadata;
import java.util.List;

/**
 * Quick Sort implementation using Lomuto partitioning and step-by-step instrumentation.
 */
public class QuickSort extends AbstractSortingAlgorithm {

    private static final AlgorithmMetadata METADATA = new AlgorithmMetadata(
            "quick-sort",
            "Quick Sort",
            AlgorithmCategory.SORTING,
            "O(n log n)",
            "O(n log n)",
            "O(n^2)",
            "O(log n)",
            "Divide-and-conquer sort that partitions elements around a pivot into smaller and larger subarrays.");

    private static final List<String> CODE_LINES = List.of(
            "void quickSort(int[] arr, int low, int high) {",
            "    if (low < high) {",
            "        int pivotIdx = partition(arr, low, high);",
            "        quickSort(arr, low, pivotIdx - 1);",
            "        quickSort(arr, pivotIdx + 1, high);",
            "    } else if (low == high) {",
            "        markSorted(low);",
            "    }",
            "}",
            "",
            "int partition(int[] arr, int low, int high) {",
            "    int pivot = arr[high];",
            "    int i = low - 1;",
            "    for (int j = low; j < high; j++) {",
            "        if (arr[j] <= pivot) {",
            "            i++;",
            "            swap(arr, i, j);",
            "        }",
            "    }",
            "    swap(arr, i + 1, high);",
            "    return i + 1;",
            "}");

    public QuickSort() {
        super(METADATA, CODE_LINES);
    }

    @Override
    protected void doSort(int[] arr, SortingStepContext ctx) {
        quickSort(arr, 0, arr.length - 1, ctx);
    }

    private void quickSort(int[] arr, int low, int high, SortingStepContext ctx) {
        if (low < high) {
            highlightLine(ctx, arr, 3, String.format("Partitioning subarray [%d .. %d]", low, high));
            int pivotIdx = partition(arr, low, high, ctx);

            if (low <= pivotIdx - 1) {
                highlightLine(ctx, arr, 4, String.format("Recursing left subarray [%d .. %d]", low, pivotIdx - 1));
            }
            quickSort(arr, low, pivotIdx - 1, ctx);

            if (pivotIdx + 1 <= high) {
                highlightLine(ctx, arr, 5, String.format("Recursing right subarray [%d .. %d]", pivotIdx + 1, high));
            }
            quickSort(arr, pivotIdx + 1, high, ctx);
        } else if (low == high && low >= 0 && low < arr.length) {
            markSorted(ctx, arr, low, 7, "Single element at index " + low + " is sorted");
        }
    }

    private int partition(int[] arr, int low, int high, SortingStepContext ctx) {
        int pivot = arr[high];
        markPivot(ctx, arr, high, 12, String.format("Selected pivot arr[%d] = %d", high, pivot));

        int i = low - 1;
        for (int j = low; j < high; j++) {
            int cmp = compare(
                    ctx,
                    arr,
                    j,
                    high,
                    15,
                    String.format("Comparing arr[%d] (%d) <= pivot (%d)", j, arr[j], pivot));
            if (cmp <= 0) {
                i++;
                if (i != j) {
                    swap(
                            ctx,
                            arr,
                            i,
                            j,
                            17,
                            String.format("Moving arr[%d] (%d) before pivot boundary", j, arr[j]));
                }
            }
        }

        int finalPivotIdx = i + 1;
        if (finalPivotIdx != high) {
            swap(
                    ctx,
                    arr,
                    finalPivotIdx,
                    high,
                    20,
                    String.format("Placing pivot %d at index %d", pivot, finalPivotIdx));
        }

        clearPivot(ctx);
        markSorted(
                ctx,
                arr,
                finalPivotIdx,
                21,
                String.format("Pivot %d locked into final position %d", arr[finalPivotIdx], finalPivotIdx));
        return finalPivotIdx;
    }
}
