package com.algolens;

import com.algolens.algorithm.AbstractAlgorithm;
import com.algolens.algorithm.AlgorithmCategory;
import com.algolens.algorithm.AlgorithmRegistry;
import com.algolens.algorithm.searching.AbstractSearchingAlgorithm;
import com.algolens.algorithm.searching.BinarySearch;
import com.algolens.algorithm.searching.LinearSearch;
import com.algolens.algorithm.searching.SearchStepContext;
import com.algolens.app.AlgoLensApp;
import com.algolens.controller.ExecutionListener;
import com.algolens.controller.VisualizationController;
import com.algolens.model.ArrayModel;
import com.algolens.model.ArrayPreset;
import com.algolens.model.ElementState;
import com.algolens.model.PlaybackState;
import com.algolens.model.StepSnapshot;
import com.algolens.view.CodeViewerPanel;
import com.algolens.view.ControlToolbarPanel;
import com.algolens.view.MainWindow;
import com.algolens.view.SortingVisualizerPanel;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

/**
 * Verification suite for AlgoLens searching algorithms, step contexts,
 * controller searching lifecycle, and UI switching between Sorting and Searching.
 */
public final class SearchEngineTest {

    private SearchEngineTest() {
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(AlgoLensApp::configureLookAndFeel);
        testSearchingRegistry();
        testLinearSearchCorrectnessAndSteps();
        testBinarySearchCorrectnessAndSteps();
        testBinarySearchRejectsUnsortedArray();
        testControllerSearchingLifecycle();
        testSearchingVisualizationRendering();
        System.out.println("ALL ALGOLENS SEARCH TESTS PASSED");
    }

    private static void testSearchingRegistry() {
        AlgorithmRegistry registry = AlgorithmRegistry.createDefault();
        List<AbstractSearchingAlgorithm> searching = registry.getSearchingAlgorithms();
        assertCondition(searching.size() >= 2, "Registry should contain Linear Search and Binary Search");
        assertCondition(registry.findSearchingAlgorithm("linear-search").isPresent(), "linear-search must exist");
        assertCondition(registry.findSearchingAlgorithm("binary-search").isPresent(), "binary-search must exist");
        assertCondition(registry.getAlgorithmsByCategory(AlgorithmCategory.SEARCHING).size() >= 2,
                "Category lookup must return searching algorithms");
        assertCondition(registry.getActiveCategories().contains(AlgorithmCategory.SEARCHING),
                "Active categories must include SEARCHING");
    }

    private static void testLinearSearchCorrectnessAndSteps() {
        LinearSearch linearSearch = new LinearSearch();
        int[] sample = {42, 12, 89, 5, 23, 12};

        verifySearchRun(linearSearch, sample, 42, 0);
        verifySearchRun(linearSearch, sample, 12, 1); // first occurrence
        verifySearchRun(linearSearch, sample, 5, 3);
        verifySearchRun(linearSearch, sample, 99, -1); // absent
        verifySearchRun(linearSearch, new int[] {7}, 7, 0);
        verifySearchRun(linearSearch, new int[] {7}, 3, -1);
    }

    private static void testBinarySearchCorrectnessAndSteps() {
        BinarySearch binarySearch = new BinarySearch();
        int[] sorted = {5, 12, 23, 42, 67, 89, 95};

        verifySearchRun(binarySearch, sorted, 5, 0);
        verifySearchRun(binarySearch, sorted, 42, 3);
        verifySearchRun(binarySearch, sorted, 95, 6);
        verifySearchRun(binarySearch, sorted, 1, -1);
        verifySearchRun(binarySearch, sorted, 50, -1);
        verifySearchRun(binarySearch, sorted, 100, -1);
        verifySearchRun(binarySearch, new int[] {15}, 15, 0);
        verifySearchRun(binarySearch, new int[] {15}, 99, -1);

        // Verify logarithmic comparison bound on 100 elements
        int[] large = new int[100];
        for (int i = 0; i < large.length; i++) {
            large[i] = i * 3 + 1;
        }
        List<StepSnapshot> snapshots = new ArrayList<>();
        SearchStepContext ctx = new SearchStepContext(large.length, 298, snapshots::add);
        int foundIdx = binarySearch.search(large, 298, ctx);
        assertCondition(foundIdx == 99, "BinarySearch should find last element in 100-element sorted array");
        assertCondition(ctx.getComparisons() <= 8,
                "BinarySearch on 100 elements should use <= 8 comparisons, used " + ctx.getComparisons());
    }

    private static void testBinarySearchRejectsUnsortedArray() {
        BinarySearch binarySearch = new BinarySearch();
        int[] unsorted = {42, 12, 89, 5, 23};
        boolean threw = false;
        try {
            binarySearch.search(unsorted, 12, new SearchStepContext(unsorted.length, 12, s -> {}));
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        assertCondition(threw, "BinarySearch must reject unsorted arrays with IllegalArgumentException");
    }

    private static void verifySearchRun(
            AbstractSearchingAlgorithm algorithm,
            int[] input,
            int target,
            int expectedIndex) {
        int[] snapshotInput = Arrays.copyOf(input, input.length);
        List<StepSnapshot> snapshots = new ArrayList<>();
        SearchStepContext ctx = new SearchStepContext(snapshotInput.length, target, snapshots::add);
        int actualIndex = algorithm.search(snapshotInput, target, ctx);

        assertCondition(actualIndex == expectedIndex,
                algorithm.getDisplayName() + " returned " + actualIndex + " expected " + expectedIndex);
        assertCondition(Arrays.equals(snapshotInput, input),
                algorithm.getDisplayName() + " must not mutate input array");
        assertCondition(!snapshots.isEmpty(), "Search must emit at least one StepSnapshot");

        int codeLineCount = algorithm.getCodeLines().size();
        boolean sawComparing = false;
        boolean sawDiscarded = false;
        for (StepSnapshot snap : snapshots) {
            int line = snap.getActiveLine();
            assertCondition(line == -1 || (line >= 1 && line <= codeLineCount),
                    algorithm.getDisplayName() + " emitted out-of-range active line: " + line);
            for (ElementState state : snap.getStates()) {
                if (state == ElementState.COMPARING) {
                    sawComparing = true;
                } else if (state == ElementState.DISCARDED) {
                    sawDiscarded = true;
                }
            }
        }

        assertCondition(sawComparing, algorithm.getDisplayName() + " must emit COMPARING state");
        if (expectedIndex == -1 || input.length > 1 && expectedIndex != 0) {
            // For absent targets or non-immediate hits, discarded state must be observed
            if (algorithm instanceof LinearSearch && expectedIndex != 0) {
                assertCondition(sawDiscarded, "LinearSearch should mark checked non-matching indices DISCARDED");
            }
        }

        StepSnapshot last = snapshots.get(snapshots.size() - 1);
        if (expectedIndex >= 0) {
            assertCondition(last.getStateAt(expectedIndex) == ElementState.FOUND,
                    "Final snapshot must mark located index as FOUND");
            assertCondition(last.getFoundIndex() == expectedIndex,
                    "Final snapshot foundIndex must match returned index");
        } else {
            assertCondition(last.getFoundIndex() == -1, "Absent target must keep foundIndex == -1");
            for (ElementState state : last.getStates()) {
                assertCondition(state == ElementState.DISCARDED,
                        "Absent target final snapshot must mark all elements DISCARDED");
            }
        }
    }

    private static void testControllerSearchingLifecycle() throws Exception {
        ArrayModel model = new ArrayModel();
        model.setCustomValues(new int[] {42, 12, 89, 5, 23});

        BinarySearch binarySearch = new BinarySearch();
        VisualizationController controller = new VisualizationController(model, new LinearSearch());
        controller.setStepDelayMs(5);

        CountDownLatch binaryDoneLatch = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            controller.setAlgorithm(binarySearch);
            assertCondition(model.getActiveCategory() == AlgorithmCategory.SEARCHING,
                    "Active category should be SEARCHING");
            assertCondition(ArrayModel.isSortedAscending(model.getValuesCopy()),
                    "Switching to BinarySearch must automatically sort unsorted model values");

            controller.setSearchTarget(42);
            assertCondition(controller.getSearchTarget() == 42, "Controller search target should update to 42");

            controller.addListener(new ExecutionListener() {
                @Override
                public void onPlaybackStateChanged(PlaybackState state) {
                    if (state == PlaybackState.COMPLETED) {
                        binaryDoneLatch.countDown();
                    }
                }
            });
            controller.play();
        });

        assertCondition(binaryDoneLatch.await(10, TimeUnit.SECONDS), "BinarySearch run should complete");

        SwingUtilities.invokeAndWait(() -> {
            assertCondition(model.getFoundIndex() == 3,
                    "42 should be located at index 3 in sorted [5, 12, 23, 42, 89]");
            assertCondition(model.getStateAt(3) == ElementState.FOUND,
                    "Index 3 should have ElementState.FOUND");
            controller.reset();
            assertCondition(controller.getPlaybackState() == PlaybackState.IDLE, "Reset should return IDLE");
            assertCondition(model.getFoundIndex() == -1, "Reset must clear foundIndex");
            controller.shutdown();
        });
    }

    private static void testSearchingVisualizationRendering() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ArrayModel model = new ArrayModel();
            model.setCustomValues(new int[] {5, 12, 23, 42, 67, 89});
            model.setActiveCategory(AlgorithmCategory.SEARCHING);
            model.setSearchTarget(42);

            ElementState[] states = {
                ElementState.DISCARDED,
                ElementState.DISCARDED,
                ElementState.DEFAULT,
                ElementState.COMPARING,
                ElementState.DEFAULT,
                ElementState.FOUND
            };
            model.applySnapshot(new StepSnapshot(
                    model.getValuesCopy(), states, 5, "Searching [2..5], mid=3", 2, 0, 3, 2, 5, 3, 5));

            SortingVisualizerPanel panel = new SortingVisualizerPanel(model);
            panel.setSize(760, 480);
            BufferedImage image = new BufferedImage(760, 480, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            panel.paint(g2);
            g2.dispose();

            CodeViewerPanel codeViewer = new CodeViewerPanel();
            for (AbstractAlgorithm alg : List.of(new LinearSearch(), new BinarySearch())) {
                codeViewer.setAlgorithm(alg);
                for (int line = 1; line <= alg.getCodeLines().size(); line++) {
                    codeViewer.setActiveLine(line);
                }
            }

            if (!GraphicsEnvironment.isHeadless()) {
                AlgorithmRegistry registry = AlgorithmRegistry.createDefault();
                VisualizationController controller =
                        new VisualizationController(model, registry.getSearchingAlgorithms().get(0));
                ControlToolbarPanel toolbar = new ControlToolbarPanel(registry, controller);
                MainWindow window = new MainWindow(registry, model, controller);
                window.setSize(1280, 800);
                window.validate();
                BufferedImage winImg = new BufferedImage(1280, 800, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gWin = winImg.createGraphics();
                window.printAll(gWin);
                gWin.dispose();
                assertCondition(toolbar.getComponentCount() > 0, "Toolbar should initialize cleanly");
                controller.shutdown();
                window.dispose();
            }
        });
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
