package com.algolens.algorithm.searching;

import com.algolens.algorithm.AlgorithmCategory;
import com.algolens.algorithm.AlgorithmMetadata;
import java.util.List;

/**
 * Linear Search implementation scanning left-to-right with step-by-step instrumentation.
 */
public class LinearSearch extends AbstractSearchingAlgorithm {

    private static final AlgorithmMetadata METADATA = new AlgorithmMetadata(
            "linear-search",
            "Linear Search",
            AlgorithmCategory.SEARCHING,
            "O(1)",
            "O(n)",
            "O(n)",
            "O(1)",
            "Sequentially inspects each element from left to right until the target is located or the array ends.");

    private static final List<String> CODE_LINES = List.of(
            "int linearSearch(int[] arr, int target) {",
            "    for (int i = 0; i < arr.length; i++) {",
            "        if (arr[i] == target) {",
            "            return i;",
            "        }",
            "        markDiscarded(i);",
            "    }",
            "    return -1;",
            "}");

    public LinearSearch() {
        super(METADATA, CODE_LINES);
    }

    @Override
    protected int doSearch(int[] arr, int target, SearchStepContext ctx) {
        for (int i = 0; i < arr.length; i++) {
            int cmp = compareAt(
                    ctx,
                    arr,
                    i,
                    3,
                    String.format("Comparing arr[%d] (%d) == target (%d)", i, arr[i], target));
            if (cmp == 0) {
                markFound(
                        ctx,
                        arr,
                        i,
                        4,
                        String.format(
                                "Target %d found at index %d (%d comparisons).",
                                target, i, ctx.getComparisons()));
                return i;
            }
            markDiscarded(
                    ctx,
                    arr,
                    i,
                    6,
                    String.format("arr[%d] (%d) != %d — marking index %d checked", i, arr[i], target, i));
        }

        markNotFound(
                ctx,
                arr,
                8,
                String.format("Target %d not found in array (%d comparisons).", target, ctx.getComparisons()));
        return -1;
    }
}
