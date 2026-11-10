package com.algolens.app;

import com.algolens.algorithm.AlgorithmRegistry;
import com.algolens.algorithm.sorting.AbstractSortingAlgorithm;
import com.algolens.controller.VisualizationController;
import com.algolens.model.ArrayModel;
import com.algolens.view.AlgoLensTheme;
import com.algolens.view.MainWindow;
import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.Insets;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point for AlgoLens.
 */
public final class AlgoLensApp {

    private AlgoLensApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AlgoLensApp::launchApplication);
    }

    private static void launchApplication() {
        configureLookAndFeel();

        AlgorithmRegistry registry = AlgorithmRegistry.createDefault();
        List<AbstractSortingAlgorithm> algorithms = registry.getSortingAlgorithms();
        if (algorithms.isEmpty()) {
            throw new IllegalStateException("No sorting algorithms registered in AlgorithmRegistry");
        }

        ArrayModel model = new ArrayModel();
        VisualizationController controller = new VisualizationController(model, algorithms.get(0));
        MainWindow mainWindow = new MainWindow(registry, model, controller);
        mainWindow.setVisible(true);
    }

    /**
     * Initializes FlatLaf Dark Mode with rounded UI styling and clean padding prior to window creation.
     */
    public static void configureLookAndFeel() {
        FlatDarkLaf.setup();

        UIManager.put("Button.arc", 10);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ProgressBar.arc", 10);
        UIManager.put("Button.margin", new Insets(6, 14, 6, 14));
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Component.accentColor", AlgoLensTheme.ACCENT_PRIMARY);
        UIManager.put("Component.focusColor", AlgoLensTheme.ACCENT_PRIMARY);

        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.track", AlgoLensTheme.BG_CANVAS);
        UIManager.put("ScrollBar.thumb", AlgoLensTheme.BORDER_SUBTLE);
        UIManager.put("ScrollBar.hoverThumbColor", AlgoLensTheme.TEXT_MUTED);

        UIManager.put("Panel.background", AlgoLensTheme.BG_SURFACE);
        UIManager.put("RootPane.background", AlgoLensTheme.BG_WINDOW);
        UIManager.put("SplitPane.background", AlgoLensTheme.BG_WINDOW);
        UIManager.put("SplitPaneDivider.gripDotCount", 3);

        UIManager.put("ComboBox.background", AlgoLensTheme.BG_ELEVATED);
        UIManager.put("ComboBox.foreground", AlgoLensTheme.TEXT_PRIMARY);
        UIManager.put("ComboBox.popupBackground", AlgoLensTheme.BG_ELEVATED);
        UIManager.put("ComboBox.selectionBackground", AlgoLensTheme.ACCENT_PRIMARY_DARK);
        UIManager.put("ComboBox.selectionForeground", AlgoLensTheme.BG_WINDOW);
        UIManager.put("PopupMenu.background", AlgoLensTheme.BG_ELEVATED);
        UIManager.put("List.background", AlgoLensTheme.BG_ELEVATED);
        UIManager.put("List.foreground", AlgoLensTheme.TEXT_PRIMARY);
        UIManager.put("List.selectionBackground", AlgoLensTheme.ACCENT_PRIMARY_DARK);
        UIManager.put("List.selectionForeground", AlgoLensTheme.BG_WINDOW);

        UIManager.put("TextField.background", AlgoLensTheme.BG_ELEVATED);
        UIManager.put("TextField.foreground", AlgoLensTheme.TEXT_PRIMARY);
        UIManager.put("TextField.caretForeground", AlgoLensTheme.ACCENT_PRIMARY);
        UIManager.put("TextField.placeholderForeground", AlgoLensTheme.TEXT_MUTED);

        UIManager.put("OptionPane.background", AlgoLensTheme.BG_SURFACE);
        UIManager.put("OptionPane.messageForeground", AlgoLensTheme.TEXT_PRIMARY);
        UIManager.put("TitlePane.unifiedBackground", true);
    }
}
