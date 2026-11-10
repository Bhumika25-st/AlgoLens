package com.algolens.view;

import com.algolens.algorithm.AlgorithmCategory;
import com.algolens.model.ArrayModel;
import com.algolens.model.PlaybackState;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Bottom status bar displaying current playback state, step narration, and live metrics.
 */
public class StatusMetricsPanel extends JPanel {

    private final StateDotIndicator stateDot = new StateDotIndicator();
    private final JLabel stateLabel = new JLabel("Ready");
    private final JLabel statusMessageLabel = new JLabel();
    private final JLabel stepsLabel = createMetricLabel();
    private final JLabel comparisonsLabel = createMetricLabel();
    private final JLabel swapsLabel = createMetricLabel();
    private final JLabel sizeLabel = createMetricLabel();

    public StatusMetricsPanel() {
        setLayout(new BorderLayout(16, 0));
        setBackground(AlgoLensTheme.BG_SURFACE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, AlgoLensTheme.BORDER_SUBTLE),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        stateLabel.setFont(AlgoLensTheme.FONT_SMALL_BOLD);
        stateLabel.setForeground(AlgoLensTheme.TEXT_PRIMARY);

        statusMessageLabel.setFont(AlgoLensTheme.FONT_UI);
        statusMessageLabel.setForeground(AlgoLensTheme.TEXT_SECONDARY);

        leftPanel.add(stateDot);
        leftPanel.add(stateLabel);
        leftPanel.add(createDivider());
        leftPanel.add(statusMessageLabel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(stepsLabel);
        rightPanel.add(comparisonsLabel);
        rightPanel.add(swapsLabel);
        rightPanel.add(sizeLabel);

        add(leftPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        updatePlaybackState(PlaybackState.IDLE);
    }

    public void updateFromModel(ArrayModel model) {
        statusMessageLabel.setText(model.getStatusMessage());
        stepsLabel.setText(String.format("Steps: %,d", model.getStepNumber()));
        comparisonsLabel.setText(String.format("Comparisons: %,d", model.getComparisons()));
        if (model.getActiveCategory() == AlgorithmCategory.SEARCHING) {
            swapsLabel.setText("Target: " + model.getSearchTarget());
            swapsLabel.setForeground(AlgoLensTheme.ACCENT_PRIMARY);
        } else {
            swapsLabel.setText(String.format("Swaps: %,d", model.getSwaps()));
            swapsLabel.setForeground(AlgoLensTheme.TEXT_PRIMARY);
        }
        sizeLabel.setText(String.format("Elements: %d", model.getSize()));
    }

    public void updatePlaybackState(PlaybackState state) {
        stateLabel.setText(state.getDisplayName());
        Color dotColor = switch (state) {
            case IDLE -> AlgoLensTheme.TEXT_MUTED;
            case RUNNING -> AlgoLensTheme.ACCENT_PRIMARY;
            case PAUSED -> AlgoLensTheme.ACCENT_WARNING;
            case COMPLETED -> AlgoLensTheme.ACCENT_SUCCESS;
        };
        stateDot.setDotColor(dotColor);
    }

    private static JLabel createMetricLabel() {
        JLabel label = new JLabel();
        label.setFont(AlgoLensTheme.FONT_MONO);
        label.setForeground(AlgoLensTheme.TEXT_PRIMARY);
        return label;
    }

    private static JLabel createDivider() {
        JLabel divider = new JLabel("|");
        divider.setFont(AlgoLensTheme.FONT_UI);
        divider.setForeground(AlgoLensTheme.BORDER_SUBTLE);
        return divider;
    }

    private static final class StateDotIndicator extends JPanel {

        private Color dotColor = AlgoLensTheme.TEXT_MUTED;

        private StateDotIndicator() {
            setOpaque(false);
            setPreferredSize(new java.awt.Dimension(10, 10));
        }

        private void setDotColor(Color dotColor) {
            this.dotColor = dotColor;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(dotColor);
            g2.fillOval(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }
}
