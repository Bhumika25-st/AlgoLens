package com.algolens;

import com.algolens.algorithm.AlgorithmRegistry;
import com.algolens.algorithm.sorting.AbstractSortingAlgorithm;
import com.algolens.algorithm.sorting.BubbleSort;
import com.algolens.algorithm.sorting.QuickSort;
import com.algolens.algorithm.sorting.SortingStepContext;
import com.algolens.app.AlgoLensApp;
import com.algolens.controller.VisualizationController;
import com.algolens.model.ArrayModel;
import com.algolens.model.ArrayPreset;
import com.algolens.model.ElementState;
import com.algolens.model.PlaybackState;
import com.algolens.model.StepSnapshot;
import com.algolens.view.AlgoLensTheme;
import com.algolens.view.CodeViewerPanel;
import com.algolens.view.ControlToolbarPanel;
import com.algolens.view.MainWindow;
import com.algolens.view.SortingVisualizerPanel;
import com.algolens.view.StatusMetricsPanel;
import com.formdev.flatlaf.FlatLaf;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Self-contained verification suite for AlgoLens sorting engine, model invariants,
 * synchronized code line bounds, and controller threading.
 */
public final class SortingEngineTest {

    private SortingEngineTest() {
    }

    public static void main(String[] args) throws Exception {
        testFlatLafInitialization();
        testAlgorithmRegistry();
        testSortingCorrectnessAndStepInvariants();
        testArrayModelPresetsAndReset();
        testCustomArrayParsingAndControllerLoading();
        testControllerLifecycleAndPauseResume();
        testControllerCancelAndAlgorithmSwitch();
        testViewComponentsAndRendering();
        System.out.println("ALL ALGOLENS TESTS PASSED");
    }

    private static void testFlatLafInitialization() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AlgoLensApp.configureLookAndFeel();
            assertCondition(FlatLaf.isLafDark(), "FlatDarkLaf must be active after configureLookAndFeel");
            assertCondition(UIManager.getLookAndFeel().getName().contains("FlatLaf"),
                    "Active LookAndFeel should be FlatLaf");
        });
    }

    private static void testAlgorithmRegistry() {
        AlgorithmRegistry registry = AlgorithmRegistry.createDefault();
        List<AbstractSortingAlgorithm> algorithms = registry.getSortingAlgorithms();
        assertCondition(algorithms.size() >= 2, "Registry should contain Bubble Sort and Quick Sort");
        assertCondition(registry.findSortingAlgorithm("bubble-sort").isPresent(), "bubble-sort must be registered");
        assertCondition(registry.findSortingAlgorithm("quick-sort").isPresent(), "quick-sort must be registered");
    }

    private static void testSortingCorrectnessAndStepInvariants() {
        List<AbstractSortingAlgorithm> algorithms = List.of(new BubbleSort(), new QuickSort());
        Random random = new Random(42L);

        int[][] edgeCases = {
            {5},
            {9, 2},
            {1, 2, 3, 4, 5},
            {5, 4, 3, 2, 1},
            {7, 3, 7, 1, 3, 7, 2}
        };

        for (AbstractSortingAlgorithm algorithm : algorithms) {
            for (int[] sample : edgeCases) {
                verifyAlgorithmRun(algorithm, sample);
            }
            for (int size : new int[] {10, 25, 50}) {
                int[] randomArr = new int[size];
                for (int i = 0; i < size; i++) {
                    randomArr[i] = 10 + random.nextInt(91);
                }
                verifyAlgorithmRun(algorithm, randomArr);
            }
        }
    }

    private static void verifyAlgorithmRun(AbstractSortingAlgorithm algorithm, int[] input) {
        int[] expected = Arrays.copyOf(input, input.length);
        Arrays.sort(expected);

        int[] working = Arrays.copyOf(input, input.length);
        List<StepSnapshot> snapshots = new ArrayList<>();
        SortingStepContext ctx = new SortingStepContext(working.length, snapshots::add);
        algorithm.sort(working, ctx);

        assertCondition(Arrays.equals(working, expected),
                algorithm.getDisplayName() + " failed to sort " + Arrays.toString(input));
        assertCondition(!snapshots.isEmpty(), "Expected at least one StepSnapshot");

        int codeLineCount = algorithm.getCodeLines().size();
        boolean sawCompare = false;
        boolean sawPivot = false;

        for (StepSnapshot snap : snapshots) {
            int line = snap.getActiveLine();
            assertCondition(line == -1 || (line >= 1 && line <= codeLineCount),
                    algorithm.getDisplayName() + " emitted out-of-range active line: " + line);
            assertCondition(snap.getLength() == input.length, "Snapshot length mismatch");
            for (ElementState state : snap.getStates()) {
                if (state == ElementState.COMPARING) {
                    sawCompare = true;
                } else if (state == ElementState.PIVOT) {
                    sawPivot = true;
                }
            }
        }

        if (input.length >= 2) {
            assertCondition(sawCompare, algorithm.getDisplayName() + " should emit COMPARING state");
        }
        if (algorithm instanceof QuickSort && input.length >= 2) {
            assertCondition(sawPivot, "QuickSort should emit PIVOT state");
        }

        StepSnapshot last = snapshots.get(snapshots.size() - 1);
        assertCondition(Arrays.equals(last.getValues(), expected), "Final snapshot must contain sorted array");
        for (ElementState state : last.getStates()) {
            assertCondition(state == ElementState.SORTED, "Final snapshot must mark all elements SORTED");
        }

        // Verify defensive copying on StepSnapshot
        int[] copy = last.getValues();
        copy[0] = -999;
        assertCondition(last.getValueAt(0) != -999, "StepSnapshot values must be defensively copied");
    }

    private static void testArrayModelPresetsAndReset() {
        ArrayModel model = new ArrayModel();
        for (ArrayPreset preset : ArrayPreset.values()) {
            model.generate(24, preset);
            assertCondition(model.getSize() == 24, "Model size mismatch for preset " + preset);
            for (ElementState state : model.getStatesCopy()) {
                assertCondition(state == ElementState.DEFAULT, "Initial generated states must be DEFAULT");
            }
        }

        int[] before = model.getValuesCopy();
        StepSnapshot mutated = new StepSnapshot(
                new int[24],
                model.getStatesCopy(),
                2,
                "mutated",
                5,
                2,
                1);
        model.applySnapshot(mutated);
        assertCondition(model.getActiveLine() == 2, "Active line should update from snapshot");
        model.resetToInitial();
        assertCondition(Arrays.equals(model.getValuesCopy(), before), "resetToInitial must restore original array");
        assertCondition(model.getActiveLine() == -1, "resetToInitial must clear active line");
    }

    private static void testCustomArrayParsingAndControllerLoading() throws Exception {
        int[] parsed = ArrayModel.parseCustomValues(" 42, 12 ,89, 5, 23 ");
        assertCondition(Arrays.equals(parsed, new int[] {42, 12, 89, 5, 23}),
                "Should parse whitespace-padded comma-separated integers");

        int[] negatives = ArrayModel.parseCustomValues("-10, 0, 25, -3");
        assertCondition(Arrays.equals(negatives, new int[] {-10, 0, 25, -3}),
                "Should support zero and negative integers");

        String[] invalidInputs = {
            null,
            "",
            "   ",
            "42",
            "42, abc, 5",
            "42; 12; 5",
            "42.5, 10, 3",
            "42, , 5",
            "42, 12,",
            "999999999999999, 5"
        };
        for (String invalid : invalidInputs) {
            boolean threw = false;
            try {
                ArrayModel.parseCustomValues(invalid);
            } catch (IllegalArgumentException expected) {
                threw = true;
            }
            assertCondition(threw, "Expected IllegalArgumentException for invalid input: " + invalid);
        }

        // Verify max size guard (> 100 elements)
        StringBuilder tooMany = new StringBuilder();
        for (int i = 0; i <= ArrayModel.MAX_ARRAY_SIZE; i++) {
            if (i > 0) {
                tooMany.append(',');
            }
            tooMany.append(i + 1);
        }
        boolean threwOverflow = false;
        try {
            ArrayModel.parseCustomValues(tooMany.toString());
        } catch (IllegalArgumentException expected) {
            threwOverflow = true;
        }
        assertCondition(threwOverflow, "Should reject custom input exceeding MAX_ARRAY_SIZE");

        // Verify loading custom dataset cancels active run, updates model, and resets to IDLE
        ArrayModel model = new ArrayModel();
        VisualizationController controller = new VisualizationController(model, new BubbleSort());
        controller.setStepDelayMs(5);

        CountDownLatch sortedCustomLatch = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            controller.play();
            controller.loadCustomArray(new int[] {42, 12, 89, 5, 23});
            assertCondition(controller.getPlaybackState() == PlaybackState.IDLE,
                    "loadCustomArray must reset playback state to IDLE");
            assertCondition(Arrays.equals(model.getValuesCopy(), new int[] {42, 12, 89, 5, 23}),
                    "Model should hold the loaded custom values");
            assertCondition(model.getStepNumber() == 0 && model.getActiveLine() == -1,
                    "loadCustomArray must reset step counters and active line");

            controller.addListener(new com.algolens.controller.ExecutionListener() {
                @Override
                public void onPlaybackStateChanged(PlaybackState state) {
                    if (state == PlaybackState.COMPLETED) {
                        sortedCustomLatch.countDown();
                    }
                }
            });
            controller.play();
        });

        assertCondition(sortedCustomLatch.await(10, TimeUnit.SECONDS), "Custom dataset sort should complete");
        SwingUtilities.invokeAndWait(() -> {
            assertCondition(Arrays.equals(model.getValuesCopy(), new int[] {5, 12, 23, 42, 89}),
                    "Custom array must sort accurately");
            controller.reset();
            assertCondition(Arrays.equals(model.getValuesCopy(), new int[] {42, 12, 89, 5, 23}),
                    "Reset after custom sort must restore custom initial dataset");
            controller.shutdown();
        });
    }

    private static void testControllerLifecycleAndPauseResume() throws Exception {
        ArrayModel model = new ArrayModel();
        model.generate(12, ArrayPreset.REVERSED);
        VisualizationController controller = new VisualizationController(model, new QuickSort());
        controller.setStepDelayMs(5);

        CountDownLatch completedLatch = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            controller.addListener(new com.algolens.controller.ExecutionListener() {
                @Override
                public void onPlaybackStateChanged(PlaybackState state) {
                    if (state == PlaybackState.COMPLETED) {
                        completedLatch.countDown();
                    }
                }
            });

            controller.stepForward();
            assertCondition(controller.getPlaybackState() == PlaybackState.PAUSED,
                    "Single step from IDLE should transition to PAUSED");

            controller.stepForward();
            assertCondition(controller.getPlaybackState() == PlaybackState.PAUSED,
                    "Subsequent single step should remain PAUSED");

            controller.play();
            assertCondition(controller.getPlaybackState() == PlaybackState.RUNNING,
                    "Play from PAUSED should transition to RUNNING");
        });

        boolean finished = completedLatch.await(10, TimeUnit.SECONDS);
        assertCondition(finished, "Controller should complete sorting within timeout");

        SwingUtilities.invokeAndWait(() -> {
            int[] sorted = model.getValuesCopy();
            for (int i = 1; i < sorted.length; i++) {
                assertCondition(sorted[i - 1] <= sorted[i], "Array must be sorted after controller completion");
            }
            controller.reset();
            assertCondition(controller.getPlaybackState() == PlaybackState.IDLE,
                    "Reset must return controller to IDLE");
            controller.shutdown();
        });
    }

    private static void testControllerCancelAndAlgorithmSwitch() throws Exception {
        ArrayModel model = new ArrayModel();
        model.generate(40, ArrayPreset.REVERSED);
        BubbleSort bubbleSort = new BubbleSort();
        QuickSort quickSort = new QuickSort();
        VisualizationController controller = new VisualizationController(model, bubbleSort);
        controller.setStepDelayMs(50);

        SwingUtilities.invokeAndWait(() -> {
            controller.play();
            assertCondition(controller.getPlaybackState() == PlaybackState.RUNNING, "Should be RUNNING");
            controller.setStepDelayMs(10);
            assertCondition(controller.getStepDelayMs() == 10, "Speed delay should update dynamically");
            controller.setAlgorithm(quickSort);
            assertCondition(controller.getCurrentAlgorithm() == quickSort, "Algorithm should switch to QuickSort");
            assertCondition(controller.getPlaybackState() == PlaybackState.IDLE,
                    "Switching algorithm must cancel worker and return to IDLE");
            assertCondition(model.getActiveLine() == -1, "Switching algorithm must clear active line");
            controller.shutdown();
        });
    }

    private static void testViewComponentsAndRendering() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ArrayModel model = new ArrayModel();
            model.generate(16, ArrayPreset.RANDOM);
            ElementState[] states = model.getStatesCopy();
            states[0] = ElementState.COMPARING;
            states[1] = ElementState.SWAPPING;
            states[2] = ElementState.PIVOT;
            states[3] = ElementState.SORTED;
            model.applySnapshot(new StepSnapshot(
                    model.getValuesCopy(), states, 6, "Test render", 1, 1, 1));

            SortingVisualizerPanel visualizer = new SortingVisualizerPanel(model);
            visualizer.setSize(800, 480);
            BufferedImage canvas = new BufferedImage(800, 480, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = canvas.createGraphics();
            visualizer.paint(g2);
            g2.dispose();
            boolean foundPaintedBarPixel = false;
            int canvasBgRgb = AlgoLensTheme.BG_CANVAS.getRGB();
            for (int x = 30; x < 770; x += 5) {
                if (canvas.getRGB(x, 430) != canvasBgRgb) {
                    foundPaintedBarPixel = true;
                    break;
                }
            }
            assertCondition(foundPaintedBarPixel,
                    "Custom SortingVisualizerPanel must render bars visibly distinct from canvas background");

            // Also verify dense array rendering (100 elements) and small/negative custom rendering
            model.generate(100, ArrayPreset.NEARLY_SORTED);
            BufferedImage denseCanvas = new BufferedImage(600, 320, BufferedImage.TYPE_INT_ARGB);
            visualizer.setSize(600, 320);
            Graphics2D gDense = denseCanvas.createGraphics();
            visualizer.paint(gDense);
            gDense.dispose();

            model.setCustomValues(new int[] {-10, 0, 5, 23, -3});
            BufferedImage customCanvas = new BufferedImage(600, 320, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gCustom = customCanvas.createGraphics();
            visualizer.paint(gCustom);
            gCustom.dispose();

            // Verify CodeViewerPanel highlights all lines without throwing
            CodeViewerPanel codeViewer = new CodeViewerPanel();
            codeViewer.setSize(420, 480);
            for (AbstractSortingAlgorithm alg : List.of(new BubbleSort(), new QuickSort())) {
                codeViewer.setAlgorithm(alg);
                for (int line = 1; line <= alg.getCodeLines().size(); line++) {
                    codeViewer.setActiveLine(line);
                }
                codeViewer.setActiveLine(-1);
            }

            if (!GraphicsEnvironment.isHeadless()) {
                AlgorithmRegistry registry = AlgorithmRegistry.createDefault();
                VisualizationController controller =
                        new VisualizationController(model, registry.getSortingAlgorithms().get(0));
                ControlToolbarPanel toolbar = new ControlToolbarPanel(registry, controller);
                JTextField customField = findFirstComponent(toolbar, JTextField.class);
                JButton loadButton = findButtonByText(toolbar, "Load Custom");
                assertCondition(customField != null && loadButton != null,
                        "Toolbar must contain custom array field and Load Custom button");
                customField.setText("42, 12, 89, 5, 23");
                loadButton.doClick();
                assertCondition(Arrays.equals(model.getValuesCopy(), new int[] {42, 12, 89, 5, 23}),
                        "Clicking Load Custom should update ArrayModel with parsed custom values");
                controller.shutdown();

                MainWindow window = new MainWindow(registry, model, controller);
                assertCondition(window.getTitle().contains("AlgoLens"), "MainWindow title must contain AlgoLens");
                window.setSize(1280, 800);
                window.validate();
                BufferedImage windowImage = new BufferedImage(1280, 800, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gWin = windowImage.createGraphics();
                window.printAll(gWin);
                gWin.dispose();
                verifyFlatLafStylingInheritance(window);
                controller.shutdown();
                window.dispose();
            }
        });
    }

    private static void verifyFlatLafStylingInheritance(Container root) {
        assertCondition(FlatLaf.isLafDark(), "FlatDarkLaf must remain active");
        int[] counts = new int[7]; // [buttons, panels, textFields, combos, sliders, scrollPanes, textPanes]
        collectAndVerifyComponents(root, counts);
        assertCondition(counts[0] >= 5, "Expected at least 5 JButtons in MainWindow");
        assertCondition(counts[1] >= 4, "Expected at least 4 JPanels in MainWindow");
        assertCondition(counts[2] >= 1, "Expected at least 1 JTextField in MainWindow");
        assertCondition(counts[3] >= 2, "Expected at least 2 JComboBoxes in MainWindow");
        assertCondition(counts[4] >= 2, "Expected at least 2 JSliders in MainWindow");
        assertCondition(counts[5] >= 1, "Expected at least 1 JScrollPane in MainWindow");
        assertCondition(counts[6] >= 1, "Expected at least 1 JTextPane in MainWindow");
    }

    private static void collectAndVerifyComponents(Container parent, int[] counts) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JButton btn) {
                counts[0]++;
                assertCondition(btn.getUI().getClass().getName().contains("Flat"),
                        "JButton must use FlatLaf UI delegate: " + btn.getUI().getClass().getName());
            } else if (child instanceof JPanel panel) {
                counts[1]++;
                assertCondition(panel.getUI().getClass().getName().contains("Flat"),
                        "JPanel must use FlatLaf UI delegate: " + panel.getUI().getClass().getName());
                if (panel instanceof SortingVisualizerPanel) {
                    assertCondition(Color.class.cast(panel.getBackground()).equals(AlgoLensTheme.BG_CANVAS),
                            "SortingVisualizerPanel should preserve custom dark canvas background");
                } else if (panel instanceof CodeViewerPanel
                        || panel instanceof ControlToolbarPanel
                        || panel instanceof StatusMetricsPanel) {
                    assertCondition(Color.class.cast(panel.getBackground()).equals(AlgoLensTheme.BG_SURFACE),
                            panel.getClass().getSimpleName() + " should preserve dark surface background");
                }
            } else if (child instanceof JTextField tf) {
                counts[2]++;
                assertCondition(tf.getUI().getClass().getName().contains("Flat"),
                        "JTextField must use FlatLaf UI delegate: " + tf.getUI().getClass().getName());
            } else if (child instanceof JComboBox<?> cb) {
                counts[3]++;
                assertCondition(cb.getUI().getClass().getName().contains("Flat"),
                        "JComboBox must use FlatLaf UI delegate: " + cb.getUI().getClass().getName());
            } else if (child instanceof JSlider slider) {
                counts[4]++;
                assertCondition(slider.getUI().getClass().getName().contains("Flat"),
                        "JSlider must use FlatLaf UI delegate: " + slider.getUI().getClass().getName());
            } else if (child instanceof JScrollPane sp) {
                counts[5]++;
                assertCondition(sp.getUI().getClass().getName().contains("Flat"),
                        "JScrollPane must use FlatLaf UI delegate: " + sp.getUI().getClass().getName());
            } else if (child instanceof JTextPane tp) {
                counts[6]++;
                assertCondition(tp.getUI().getClass().getName().contains("Flat"),
                        "JTextPane must use FlatLaf UI delegate: " + tp.getUI().getClass().getName());
            }
            if (child instanceof Container nested) {
                collectAndVerifyComponents(nested, counts);
            }
        }
    }

    private static <T extends Component> T findFirstComponent(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container nested) {
                T found = findFirstComponent(nested, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JButton findButtonByText(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton btn && text.equals(btn.getText())) {
                return btn;
            }
            if (child instanceof Container nested) {
                JButton found = findButtonByText(nested, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
