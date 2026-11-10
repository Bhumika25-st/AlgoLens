package com.algolens.view;

import com.algolens.algorithm.AbstractAlgorithm;
import com.algolens.algorithm.AlgorithmRegistry;
import com.algolens.controller.ExecutionListener;
import com.algolens.controller.VisualizationController;
import com.algolens.model.ArrayModel;
import com.algolens.model.PlaybackState;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.text.JTextComponent;

/**
 * Main AlgoLens desktop application window assembling the control toolbar,
 * bar visualizer canvas, synchronized code viewer, and status metrics bar.
 */
public class MainWindow extends JFrame implements ArrayModel.ModelListener, ExecutionListener {

    private final ArrayModel model;
    private final VisualizationController controller;
    private final SortingVisualizerPanel visualizerPanel;
    private final CodeViewerPanel codeViewerPanel;
    private final StatusMetricsPanel statusPanel;

    public MainWindow(AlgorithmRegistry registry, ArrayModel model, VisualizationController controller) {
        super("AlgoLens — Algorithm Visualizer");
        this.model = model;
        this.controller = controller;

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 680));
        setPreferredSize(new Dimension(1280, 800));
        getContentPane().setBackground(AlgoLensTheme.BG_WINDOW);
        setLayout(new BorderLayout());

        ControlToolbarPanel toolbarPanel = new ControlToolbarPanel(registry, controller);
        this.visualizerPanel = new SortingVisualizerPanel(model);
        this.codeViewerPanel = new CodeViewerPanel();
        this.statusPanel = new StatusMetricsPanel();

        visualizerPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                getRootPane().requestFocusInWindow();
            }
        });

        codeViewerPanel.setAlgorithm(controller.getCurrentAlgorithm());
        statusPanel.updateFromModel(model);

        JSplitPane splitPane = createSplitPane(visualizerPanel, codeViewerPanel);
        JPanel workspaceWrapper = new JPanel(new BorderLayout());
        workspaceWrapper.setBackground(AlgoLensTheme.BG_WINDOW);
        workspaceWrapper.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        workspaceWrapper.add(splitPane, BorderLayout.CENTER);

        add(toolbarPanel, BorderLayout.NORTH);
        add(workspaceWrapper, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);

        model.addListener(this);
        controller.addListener(this);

        registerKeyboardShortcuts();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controller.shutdown();
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    private JSplitPane createSplitPane(JComponent left, JComponent right) {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        splitPane.setResizeWeight(0.65);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setBackground(AlgoLensTheme.BG_WINDOW);
        splitPane.setDividerSize(8);
        splitPane.putClientProperty(FlatClientProperties.STYLE, "background: #0B1120");
        return splitPane;
    }

    private void registerKeyboardShortcuts() {
        JComponent root = getRootPane();
        var inputMap = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        var actionMap = root.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "togglePlayPause");
        actionMap.put("togglePlayPause", new ShortcutAction(controller::togglePlayPause));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "stepForward");
        actionMap.put("stepForward", new ShortcutAction(controller::stepForward));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "resetArray");
        actionMap.put("resetArray", new ShortcutAction(controller::reset));
    }

    @Override
    public void onModelChanged(ArrayModel updatedModel) {
        visualizerPanel.repaint();
        codeViewerPanel.setActiveLine(updatedModel.getActiveLine());
        statusPanel.updateFromModel(updatedModel);
    }

    @Override
    public void onAlgorithmChanged(AbstractAlgorithm algorithm) {
        codeViewerPanel.setAlgorithm(algorithm);
    }

    @Override
    public void onPlaybackStateChanged(PlaybackState state) {
        statusPanel.updatePlaybackState(state);
    }

    /**
     * Global keyboard shortcut action that automatically yields when typing inside a text field.
     */
    private static final class ShortcutAction extends AbstractAction {

        private final Runnable callback;

        private ShortcutAction(Runnable callback) {
            this.callback = callback;
        }

        @Override
        public boolean isEnabled() {
            Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            return !(focusOwner instanceof JTextComponent);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            callback.run();
        }
    }
}
