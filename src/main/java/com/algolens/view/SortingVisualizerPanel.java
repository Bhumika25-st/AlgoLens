package com.algolens.view;

import com.algolens.algorithm.AlgorithmCategory;
import com.algolens.model.ArrayModel;
import com.algolens.model.ElementState;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;
import javax.swing.JPanel;

/**
 * Custom Java2D rendering panel that paints array elements as dynamic vertical bars
 * color-coded by sorting and searching execution states.
 */
public class SortingVisualizerPanel extends JPanel {

    private static final int LEGEND_HEIGHT = 36;
    private static final int PADDING_X = 24;
    private static final int PADDING_TOP = 24;
    private static final int PADDING_BOTTOM = 42;

    private final ArrayModel model;

    public SortingVisualizerPanel(ArrayModel model) {
        this.model = Objects.requireNonNull(model, "model must not be null");
        setBackground(AlgoLensTheme.BG_CANVAS);
        setBorder(AlgoLensTheme.createCardBorder());
        setPreferredSize(new Dimension(760, 480));
        setMinimumSize(new Dimension(360, 260));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            drawLegend(g2);

            int[] values = model.getValuesCopy();
            ElementState[] states = model.getStatesCopy();
            if (values.length == 0) {
                return;
            }

            int plotLeft = PADDING_X;
            int plotRight = getWidth() - PADDING_X;
            int plotTop = LEGEND_HEIGHT + PADDING_TOP;
            int plotBottom = getHeight() - PADDING_BOTTOM;
            int plotWidth = Math.max(10, plotRight - plotLeft);
            int plotHeight = Math.max(10, plotBottom - plotTop);

            drawReferenceGrid(g2, plotLeft, plotTop, plotWidth, plotHeight, plotBottom);
            drawBars(g2, values, states, plotLeft, plotTop, plotWidth, plotHeight, plotBottom);
        } finally {
            g2.dispose();
        }
    }

    private void drawLegend(Graphics2D g2) {
        g2.setColor(AlgoLensTheme.BG_SURFACE);
        g2.fillRect(0, 0, getWidth(), LEGEND_HEIGHT);
        g2.setColor(AlgoLensTheme.BORDER_SUBTLE);
        g2.drawLine(0, LEGEND_HEIGHT, getWidth(), LEGEND_HEIGHT);

        boolean searching = model.getActiveCategory() == AlgorithmCategory.SEARCHING;
        g2.setFont(AlgoLensTheme.FONT_SMALL_BOLD);
        g2.setColor(AlgoLensTheme.TEXT_SECONDARY);
        String headerTitle = searching ? "SEARCH VISUALIZATION" : "ARRAY VISUALIZATION";
        g2.drawString(headerTitle, 16, 22);

        if (searching) {
            int titleW = g2.getFontMetrics().stringWidth(headerTitle);
            g2.setColor(AlgoLensTheme.ACCENT_PRIMARY);
            g2.drawString("   •   Target: " + model.getSearchTarget(), 16 + titleW, 22);
        }

        ElementState[] legendStates = searching
                ? new ElementState[] {
                    ElementState.DEFAULT,
                    ElementState.COMPARING,
                    ElementState.DISCARDED,
                    ElementState.FOUND
                }
                : new ElementState[] {
                    ElementState.DEFAULT,
                    ElementState.COMPARING,
                    ElementState.SWAPPING,
                    ElementState.PIVOT,
                    ElementState.SORTED
                };

        FontMetrics fm = g2.getFontMetrics();
        int totalLegendWidth = 0;
        for (ElementState s : legendStates) {
            totalLegendWidth += 10 + 6 + fm.stringWidth(s.getDisplayName()) + 16;
        }

        int x = Math.max(240, getWidth() - totalLegendWidth - 8);
        g2.setFont(AlgoLensTheme.FONT_SMALL);
        for (ElementState state : legendStates) {
            Color c = AlgoLensTheme.colorForState(state);
            g2.setColor(c);
            g2.fillOval(x, 13, 10, 10);
            x += 16;
            g2.setColor(AlgoLensTheme.TEXT_SECONDARY);
            g2.drawString(state.getDisplayName(), x, 22);
            x += fm.stringWidth(state.getDisplayName()) + 16;
        }
    }

    private void drawReferenceGrid(
            Graphics2D g2, int plotLeft, int plotTop, int plotWidth, int plotHeight, int plotBottom) {
        g2.setStroke(new BasicStroke(1f));
        for (int q = 1; q <= 4; q++) {
            int y = plotBottom - (int) Math.round(plotHeight * (q / 4.0));
            g2.setColor(AlgoLensTheme.GRID_LINE);
            g2.drawLine(plotLeft, y, plotLeft + plotWidth, y);
        }
        g2.setColor(AlgoLensTheme.BORDER_SUBTLE);
        g2.drawLine(plotLeft, plotBottom, plotLeft + plotWidth, plotBottom);
    }

    private void drawBars(
            Graphics2D g2,
            int[] values,
            ElementState[] states,
            int plotLeft,
            int plotTop,
            int plotWidth,
            int plotHeight,
            int plotBottom) {
        int n = values.length;
        int minVal = model.getMinValue();
        int maxVal = model.getMaxValue();
        double slotWidth = plotWidth / (double) n;
        double gap = slotWidth >= 14 ? 4.0 : (slotWidth >= 6 ? 2.0 : (slotWidth >= 3 ? 1.0 : 0.0));
        double barWidth = Math.max(1.0, slotWidth - gap);
        boolean showValues = barWidth >= 20;
        boolean showIndices = barWidth >= 18;
        boolean showPointerText = slotWidth >= 14;

        int lowIndex = model.getLowIndex();
        int midIndex = model.getMidIndex();
        int highIndex = model.getHighIndex();

        Font valueFont = AlgoLensTheme.FONT_SMALL_BOLD;
        Font indexFont = AlgoLensTheme.FONT_SMALL;

        for (int i = 0; i < n; i++) {
            double ratio = computeHeightRatio(values[i], minVal, maxVal);
            double barHeight = Math.max(4.0, ratio * (plotHeight - (showValues ? 18 : 4)));
            double x = plotLeft + i * slotWidth + gap * 0.5;
            double y = plotBottom - barHeight;

            ElementState state = i < states.length ? states[i] : ElementState.DEFAULT;
            Color baseColor = AlgoLensTheme.colorForState(state);

            Shape barShape = barWidth >= 5.0
                    ? new RoundRectangle2D.Double(x, y, barWidth, barHeight, Math.min(6.0, barWidth * 0.45), 6.0)
                    : new Rectangle2D.Double(x, y, barWidth, barHeight);

            if (state == ElementState.DISCARDED) {
                g2.setColor(baseColor);
                g2.fill(barShape);
            } else {
                Color topColor = brighten(baseColor, 0.16f);
                g2.setPaint(new GradientPaint(
                        (float) x, (float) y, topColor,
                        (float) x, (float) plotBottom, baseColor.darker()));
                g2.fill(barShape);

                if (state == ElementState.COMPARING
                        || state == ElementState.SWAPPING
                        || state == ElementState.PIVOT
                        || state == ElementState.TARGET
                        || state == ElementState.FOUND) {
                    g2.setColor(topColor.brighter());
                    g2.setStroke(new BasicStroke(1.4f));
                    g2.draw(barShape);
                }
            }

            if (showValues) {
                g2.setFont(valueFont);
                FontMetrics fm = g2.getFontMetrics();
                String valStr = String.valueOf(values[i]);
                int textX = (int) Math.round(x + (barWidth - fm.stringWidth(valStr)) * 0.5);
                int textY = (int) Math.round(y - 5);
                Color labelColor = switch (state) {
                    case DISCARDED -> AlgoLensTheme.TEXT_MUTED;
                    case DEFAULT -> AlgoLensTheme.TEXT_SECONDARY;
                    default -> AlgoLensTheme.TEXT_PRIMARY;
                };
                g2.setColor(labelColor);
                g2.drawString(valStr, textX, textY);
            }

            if (showIndices) {
                g2.setFont(indexFont);
                FontMetrics fm = g2.getFontMetrics();
                String idxStr = String.valueOf(i);
                int idxX = (int) Math.round(x + (barWidth - fm.stringWidth(idxStr)) * 0.5);
                boolean highlighted = state == ElementState.COMPARING
                        || state == ElementState.SWAPPING
                        || state == ElementState.PIVOT
                        || state == ElementState.FOUND;
                g2.setColor(highlighted ? baseColor : AlgoLensTheme.TEXT_MUTED);
                g2.drawString(idxStr, idxX, plotBottom + 15);
            }

            String pointerTag = buildPointerTag(i, lowIndex, midIndex, highIndex);
            if (!pointerTag.isEmpty()) {
                Color pointerColor = (i == midIndex)
                        ? AlgoLensTheme.ACCENT_WARNING
                        : AlgoLensTheme.ACCENT_PRIMARY;
                int centerX = (int) Math.round(x + barWidth * 0.5);
                g2.setColor(pointerColor);
                g2.fillOval(centerX - 3, plotBottom + (showIndices ? 20 : 6), 6, 6);
                if (showPointerText) {
                    g2.setFont(AlgoLensTheme.FONT_SMALL_BOLD);
                    FontMetrics fm = g2.getFontMetrics();
                    int tagX = centerX - fm.stringWidth(pointerTag) / 2;
                    int tagY = plotBottom + (showIndices ? 36 : 22);
                    g2.drawString(pointerTag, tagX, tagY);
                }
            }
        }
    }

    private static String buildPointerTag(int index, int lowIndex, int midIndex, int highIndex) {
        StringBuilder sb = new StringBuilder();
        if (index == lowIndex) {
            sb.append("L");
        }
        if (index == midIndex) {
            if (!sb.isEmpty()) {
                sb.append('/');
            }
            sb.append("M");
        }
        if (index == highIndex) {
            if (!sb.isEmpty()) {
                sb.append('/');
            }
            sb.append("H");
        }
        return sb.toString();
    }

    private static double computeHeightRatio(int value, int minVal, int maxVal) {
        if (maxVal == minVal) {
            return 0.65;
        }
        if (minVal >= 0) {
            double safeMax = Math.max(1.0, maxVal);
            return value == 0 ? 0.04 : Math.max(0.06, value / safeMax);
        }
        double span = (double) maxVal - (double) minVal;
        return 0.08 + 0.92 * ((value - (double) minVal) / span);
    }

    private static Color brighten(Color c, float factor) {
        int r = Math.min(255, Math.round(c.getRed() + (255 - c.getRed()) * factor));
        int g = Math.min(255, Math.round(c.getGreen() + (255 - c.getGreen()) * factor));
        int b = Math.min(255, Math.round(c.getBlue() + (255 - c.getBlue()) * factor));
        return new Color(r, g, b);
    }
}
