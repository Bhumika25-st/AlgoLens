package com.algolens.view;

import com.algolens.algorithm.AbstractAlgorithm;
import com.algolens.algorithm.AlgorithmCategory;
import com.algolens.algorithm.AlgorithmRegistry;
import com.algolens.controller.ExecutionListener;
import com.algolens.controller.VisualizationController;
import com.algolens.model.ArrayModel;
import com.algolens.model.ArrayPreset;
import com.algolens.model.PlaybackState;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Top control toolbar supporting category switching (Sorting / Searching), algorithm selection,
 * dataset controls, custom array loading, search target input, speed slider, and transport actions.
 */
public class ControlToolbarPanel extends JPanel implements ExecutionListener {

    private final AlgorithmRegistry registry;
    private final VisualizationController controller;
    private final JComboBox<AlgorithmCategory> categoryCombo;
    private final JComboBox<AbstractAlgorithm> algorithmCombo;
    private final JComboBox<ArrayPreset> presetCombo;
    private final JSlider sizeSlider;
    private final JLabel sizeValueLabel;
    private final JSlider speedSlider;
    private final JLabel speedValueLabel;
    private final JTextField customArrayField;
    private final JButton loadCustomButton;
    private final JPanel targetPanel;
    private final JTextField targetField;
    private final JButton setTargetButton;
    private final JButton playPauseButton;
    private final JButton stepButton;
    private final JButton shuffleButton;
    private final JButton resetButton;

    private boolean suppressSizeSliderEvents;
    private boolean suppressComboEvents;

    public ControlToolbarPanel(AlgorithmRegistry registry, VisualizationController controller) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.controller = Objects.requireNonNull(controller, "controller must not be null");

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(AlgoLensTheme.BG_SURFACE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AlgoLensTheme.BORDER_SUBTLE),
                BorderFactory.createEmptyBorder(8, 16, 10, 16)));

        // Top row: Brand, Category, Algorithm, Dataset, and Transport Actions
        JPanel topRow = new JPanel(new BorderLayout(12, 0));
        topRow.setOpaque(false);

        JPanel selectorsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        selectorsPanel.setOpaque(false);

        JLabel brandLabel = new JLabel("AlgoLens");
        brandLabel.setFont(AlgoLensTheme.FONT_TITLE);
        brandLabel.setForeground(AlgoLensTheme.ACCENT_PRIMARY);
        brandLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        selectorsPanel.add(brandLabel);

        selectorsPanel.add(createFieldLabel("Mode"));
        categoryCombo = new JComboBox<>(registry.getActiveCategories().toArray(AlgorithmCategory[]::new));
        categoryCombo.setSelectedItem(controller.getCurrentAlgorithm().getCategory());
        AlgoLensTheme.styleComboBox(categoryCombo);
        categoryCombo.addActionListener(e -> {
            if (!suppressComboEvents) {
                AlgorithmCategory selectedCategory = (AlgorithmCategory) categoryCombo.getSelectedItem();
                if (selectedCategory != null) {
                    onCategorySelected(selectedCategory);
                }
            }
        });
        selectorsPanel.add(categoryCombo);

        selectorsPanel.add(createFieldLabel("Algorithm"));
        algorithmCombo = new JComboBox<>();
        AlgoLensTheme.styleComboBox(algorithmCombo);
        populateAlgorithmsForCategory(controller.getCurrentAlgorithm().getCategory(), controller.getCurrentAlgorithm());
        algorithmCombo.addActionListener(e -> {
            if (!suppressComboEvents) {
                AbstractAlgorithm selected = (AbstractAlgorithm) algorithmCombo.getSelectedItem();
                if (selected != null && selected != controller.getCurrentAlgorithm()) {
                    controller.setAlgorithm(selected);
                }
            }
        });
        selectorsPanel.add(algorithmCombo);

        selectorsPanel.add(createFieldLabel("Dataset"));
        presetCombo = new JComboBox<>(ArrayPreset.values());
        presetCombo.setSelectedItem(controller.getModel().getCurrentPreset());
        AlgoLensTheme.styleComboBox(presetCombo);
        presetCombo.addActionListener(e -> regenerateDataset());
        selectorsPanel.add(presetCombo);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        actionsPanel.setOpaque(false);

        playPauseButton = AlgoLensTheme.createButton(
                "Play",
                AlgoLensTheme.ACCENT_PRIMARY_DARK,
                AlgoLensTheme.ACCENT_PRIMARY,
                AlgoLensTheme.BG_WINDOW);
        playPauseButton.addActionListener(e -> handlePlayPause());

        stepButton = AlgoLensTheme.createButton(
                "Step",
                AlgoLensTheme.BG_ELEVATED,
                AlgoLensTheme.BORDER_SUBTLE,
                AlgoLensTheme.TEXT_PRIMARY);
        stepButton.addActionListener(e -> handleStepForward());

        shuffleButton = AlgoLensTheme.createButton(
                "Shuffle",
                AlgoLensTheme.BG_ELEVATED,
                AlgoLensTheme.BORDER_SUBTLE,
                AlgoLensTheme.TEXT_PRIMARY);
        shuffleButton.addActionListener(e -> regenerateDataset());

        resetButton = AlgoLensTheme.createButton(
                "Reset",
                AlgoLensTheme.BG_ELEVATED,
                AlgoLensTheme.BORDER_SUBTLE,
                AlgoLensTheme.TEXT_PRIMARY);
        resetButton.addActionListener(e -> controller.reset());

        actionsPanel.add(playPauseButton);
        actionsPanel.add(stepButton);
        actionsPanel.add(shuffleButton);
        actionsPanel.add(resetButton);

        topRow.add(selectorsPanel, BorderLayout.WEST);
        topRow.add(actionsPanel, BorderLayout.EAST);

        // Second row: Sliders and Shortcut Hints
        JPanel middleRow = new JPanel(new BorderLayout(12, 0));
        middleRow.setOpaque(false);
        middleRow.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        JPanel slidersPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        slidersPanel.setOpaque(false);

        slidersPanel.add(createFieldLabel("Array Size"));
        sizeSlider = new JSlider(
                ArrayModel.MIN_ARRAY_SIZE,
                ArrayModel.MAX_ARRAY_SIZE,
                controller.getModel().getSize());
        sizeSlider.setPreferredSize(new Dimension(140, 22));
        AlgoLensTheme.styleSlider(sizeSlider);
        sizeValueLabel = createReadoutLabel(String.valueOf(sizeSlider.getValue()), 32);
        sizeSlider.addChangeListener(e -> {
            int size = sizeSlider.getValue();
            sizeValueLabel.setText(String.valueOf(size));
            if (!suppressSizeSliderEvents && size != controller.getModel().getSize()) {
                regenerateDataset();
            }
        });
        slidersPanel.add(sizeSlider);
        slidersPanel.add(sizeValueLabel);

        slidersPanel.add(createFieldLabel("Animation Speed"));
        speedSlider = new JSlider(1, 100, delayToSliderValue(controller.getStepDelayMs()));
        speedSlider.setPreferredSize(new Dimension(145, 22));
        AlgoLensTheme.styleSlider(speedSlider);
        speedValueLabel = createReadoutLabel(controller.getStepDelayMs() + " ms", 54);
        speedSlider.addChangeListener(e -> {
            int delayMs = sliderValueToDelay(speedSlider.getValue());
            speedValueLabel.setText(delayMs + " ms");
            controller.setStepDelayMs(delayMs);
        });
        slidersPanel.add(speedSlider);
        slidersPanel.add(speedValueLabel);

        JLabel shortcutHint = new JLabel("Shortcuts:  Space = Play/Pause   |   Right Arrow = Step   |   R = Reset");
        shortcutHint.setFont(AlgoLensTheme.FONT_SMALL);
        shortcutHint.setForeground(AlgoLensTheme.TEXT_MUTED);
        shortcutHint.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));

        middleRow.add(slidersPanel, BorderLayout.WEST);
        middleRow.add(shortcutHint, BorderLayout.EAST);

        // Third row: Custom Comma-Separated Array Input + Search Target Input
        JPanel customRow = new JPanel(new BorderLayout(12, 0));
        customRow.setOpaque(false);
        customRow.setBorder(BorderFactory.createEmptyBorder(6, 10, 0, 6));

        JPanel customInputGroup = new JPanel(new BorderLayout(8, 0));
        customInputGroup.setOpaque(false);

        JLabel customLabel = createFieldLabel("Custom Array");
        customArrayField = new JTextField();
        AlgoLensTheme.styleTextField(customArrayField, "e.g. 42, 12, 89, 5, 23");
        customArrayField.setToolTipText("Enter 2 to 100 comma-separated integers (e.g., 42, 12, 89, 5, 23)");
        customArrayField.addActionListener(e -> loadCustomArray());
        attachOutlineClearListener(customArrayField);

        loadCustomButton = AlgoLensTheme.createButton(
                "Load Custom",
                AlgoLensTheme.BG_ELEVATED,
                AlgoLensTheme.BORDER_SUBTLE,
                AlgoLensTheme.ACCENT_PRIMARY);
        loadCustomButton.addActionListener(e -> loadCustomArray());

        customInputGroup.add(customLabel, BorderLayout.WEST);
        customInputGroup.add(customArrayField, BorderLayout.CENTER);
        customInputGroup.add(loadCustomButton, BorderLayout.EAST);

        targetPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        targetPanel.setOpaque(false);
        targetPanel.add(createFieldLabel("Search Target"));

        targetField = new JTextField(String.valueOf(controller.getSearchTarget()), 7);
        targetField.setPreferredSize(new Dimension(95, 30));
        AlgoLensTheme.styleTextField(targetField, "Target int");
        targetField.setToolTipText("Enter integer value to search for (e.g., 42)");
        targetField.addActionListener(e -> applyTargetInput(true));
        attachOutlineClearListener(targetField);

        setTargetButton = AlgoLensTheme.createButton(
                "Set Target",
                AlgoLensTheme.BG_ELEVATED,
                AlgoLensTheme.BORDER_SUBTLE,
                AlgoLensTheme.ACCENT_PRIMARY);
        setTargetButton.addActionListener(e -> applyTargetInput(true));

        targetPanel.add(targetField);
        targetPanel.add(setTargetButton);

        customRow.add(customInputGroup, BorderLayout.CENTER);
        customRow.add(targetPanel, BorderLayout.EAST);

        add(topRow);
        add(middleRow);
        add(customRow);

        controller.addListener(this);
        syncTargetVisibility(controller.getCurrentAlgorithm().getCategory());
        syncPlaybackButtons(controller.getPlaybackState());
    }

    private void onCategorySelected(AlgorithmCategory category) {
        List<AbstractAlgorithm> algs = registry.getAlgorithmsByCategory(category);
        if (algs.isEmpty()) {
            return;
        }
        AbstractAlgorithm nextAlg = algs.get(0);
        populateAlgorithmsForCategory(category, nextAlg);
        controller.setAlgorithm(nextAlg);
        syncTargetVisibility(category);
    }

    private void populateAlgorithmsForCategory(AlgorithmCategory category, AbstractAlgorithm activeAlgorithm) {
        suppressComboEvents = true;
        try {
            List<AbstractAlgorithm> algs = registry.getAlgorithmsByCategory(category);
            DefaultComboBoxModel<AbstractAlgorithm> comboModel =
                    new DefaultComboBoxModel<>(algs.toArray(AbstractAlgorithm[]::new));
            algorithmCombo.setModel(comboModel);
            algorithmCombo.setSelectedItem(activeAlgorithm);
        } finally {
            suppressComboEvents = false;
        }
    }

    private void syncTargetVisibility(AlgorithmCategory category) {
        boolean isSearching = (category == AlgorithmCategory.SEARCHING);
        targetPanel.setVisible(isSearching);
        if (isSearching) {
            targetField.setText(String.valueOf(controller.getSearchTarget()));
            clearFieldError(targetField);
        }
        revalidate();
        repaint();
    }

    private void handlePlayPause() {
        if (controller.getPlaybackState() != PlaybackState.RUNNING
                && controller.getPlaybackState() != PlaybackState.PAUSED
                && controller.getCurrentAlgorithm().getCategory() == AlgorithmCategory.SEARCHING) {
            if (!applyTargetInput(false)) {
                return;
            }
        }
        controller.togglePlayPause();
    }

    private void handleStepForward() {
        if ((controller.getPlaybackState() == PlaybackState.IDLE
                        || controller.getPlaybackState() == PlaybackState.COMPLETED)
                && controller.getCurrentAlgorithm().getCategory() == AlgorithmCategory.SEARCHING) {
            if (!applyTargetInput(false)) {
                return;
            }
        }
        controller.stepForward();
    }

    private boolean applyTargetInput(boolean returnFocusToRoot) {
        String raw = targetField.getText();
        if (raw == null || raw.isBlank()) {
            showInvalidTargetError("Please enter an integer target value to search for.");
            return false;
        }
        try {
            int parsedTarget = Integer.parseInt(raw.trim());
            clearFieldError(targetField);
            if (parsedTarget != controller.getSearchTarget()) {
                controller.setSearchTarget(parsedTarget);
            }
            if (returnFocusToRoot) {
                returnFocusToWindowRoot();
            }
            return true;
        } catch (NumberFormatException ex) {
            showInvalidTargetError("Invalid target '" + raw.trim() + "'. Please enter a whole integer.");
            return false;
        }
    }

    private void showInvalidTargetError(String message) {
        targetField.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
        JOptionPane.showMessageDialog(
                this,
                message,
                "Invalid Search Target",
                JOptionPane.ERROR_MESSAGE);
        targetField.requestFocusInWindow();
        targetField.selectAll();
    }

    private void regenerateDataset() {
        ArrayPreset preset = (ArrayPreset) presetCombo.getSelectedItem();
        if (preset == null) {
            preset = ArrayPreset.RANDOM;
        }
        controller.generateNewArray(sizeSlider.getValue(), preset);
    }

    private void loadCustomArray() {
        try {
            int[] customValues = ArrayModel.parseCustomValues(customArrayField.getText());
            clearFieldError(customArrayField);
            controller.loadCustomArray(customValues);
            syncSizeSliderSilently(customValues.length);
            returnFocusToWindowRoot();
        } catch (IllegalArgumentException ex) {
            customArrayField.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Invalid Custom Array",
                    JOptionPane.ERROR_MESSAGE);
            customArrayField.requestFocusInWindow();
            customArrayField.selectAll();
        }
    }

    private void returnFocusToWindowRoot() {
        JRootPane rootPane = SwingUtilities.getRootPane(this);
        if (rootPane != null) {
            rootPane.requestFocusInWindow();
        }
    }

    private void syncSizeSliderSilently(int newSize) {
        suppressSizeSliderEvents = true;
        try {
            int clamped = Math.max(ArrayModel.MIN_ARRAY_SIZE, Math.min(ArrayModel.MAX_ARRAY_SIZE, newSize));
            sizeSlider.setValue(clamped);
            sizeValueLabel.setText(String.valueOf(newSize));
        } finally {
            suppressSizeSliderEvents = false;
        }
    }

    private static void clearFieldError(JTextField field) {
        field.putClientProperty(FlatClientProperties.OUTLINE, null);
    }

    private static void attachOutlineClearListener(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                clearFieldError(field);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                clearFieldError(field);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                clearFieldError(field);
            }
        });
    }

    @Override
    public void onPlaybackStateChanged(PlaybackState state) {
        syncPlaybackButtons(state);
    }

    @Override
    public void onAlgorithmChanged(AbstractAlgorithm algorithm) {
        suppressComboEvents = true;
        try {
            if (categoryCombo.getSelectedItem() != algorithm.getCategory()) {
                categoryCombo.setSelectedItem(algorithm.getCategory());
                populateAlgorithmsForCategory(algorithm.getCategory(), algorithm);
            } else {
                algorithmCombo.setSelectedItem(algorithm);
            }
        } finally {
            suppressComboEvents = false;
        }
        syncTargetVisibility(algorithm.getCategory());
    }

    private void syncPlaybackButtons(PlaybackState state) {
        switch (state) {
            case IDLE -> {
                playPauseButton.setText("Play");
                stepButton.setEnabled(true);
            }
            case RUNNING -> {
                playPauseButton.setText("Pause");
                stepButton.setEnabled(false);
            }
            case PAUSED -> {
                playPauseButton.setText("Resume");
                stepButton.setEnabled(true);
            }
            case COMPLETED -> {
                playPauseButton.setText("Replay");
                stepButton.setEnabled(true);
            }
        }
    }

    private static int sliderValueToDelay(int sliderVal) {
        double normalized = (100 - sliderVal) / 99.0;
        int span = VisualizationController.MAX_DELAY_MS - VisualizationController.MIN_DELAY_MS;
        return VisualizationController.MIN_DELAY_MS + (int) Math.round(normalized * normalized * span);
    }

    private static int delayToSliderValue(int delayMs) {
        int span = VisualizationController.MAX_DELAY_MS - VisualizationController.MIN_DELAY_MS;
        double ratio = Math.max(0.0, Math.min(1.0, (delayMs - VisualizationController.MIN_DELAY_MS) / (double) span));
        return (int) Math.round(100 - Math.sqrt(ratio) * 99.0);
    }

    private static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(AlgoLensTheme.FONT_SMALL_BOLD);
        label.setForeground(AlgoLensTheme.TEXT_SECONDARY);
        return label;
    }

    private static JLabel createReadoutLabel(String text, int width) {
        JLabel label = new JLabel(text);
        label.setFont(AlgoLensTheme.FONT_MONO);
        label.setForeground(AlgoLensTheme.ACCENT_PRIMARY);
        label.setPreferredSize(new Dimension(width, 20));
        return label;
    }
}
