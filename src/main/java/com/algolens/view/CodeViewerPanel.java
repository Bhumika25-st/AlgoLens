package com.algolens.view;

import com.algolens.algorithm.AbstractAlgorithm;
import com.algolens.algorithm.AlgorithmMetadata;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Displays algorithm metadata, complexity badges, and a synchronized {@link JTextPane}
 * that highlights the active source line during step-by-step execution.
 */
public class CodeViewerPanel extends JPanel {

    private static final Set<String> KEYWORDS = Set.of(
            "void", "int", "boolean", "for", "if", "else", "break", "return", "true", "false");
    private static final Set<String> HELPER_CALLS = Set.of(
            "swap", "markSorted", "markDiscarded", "partition", "quickSort", "bubbleSort", "linearSearch", "binarySearch");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\b[A-Za-z_][A-Za-z0-9_]*\\b|\\b\\d+\\b");

    private final JLabel titleLabel = new JLabel();
    private final JTextArea descriptionArea = new JTextArea();
    private final JPanel complexityBadgesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final JTextPane codePane = new JTextPane();
    private final LineNumberGutter gutter = new LineNumberGutter();
    private final Highlighter.HighlightPainter activeLinePainter = new ActiveLineHighlightPainter();

    private int totalLines;
    private int activeLine = -1;
    private Object activeHighlightTag;

    public CodeViewerPanel() {
        setLayout(new BorderLayout());
        setBackground(AlgoLensTheme.BG_SURFACE);
        setBorder(AlgoLensTheme.createCardBorder());
        setPreferredSize(new Dimension(430, 480));
        setMinimumSize(new Dimension(280, 260));

        add(buildHeaderPanel(), BorderLayout.NORTH);
        add(buildCodeScrollPane(), BorderLayout.CENTER);
    }

    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(AlgoLensTheme.BG_SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AlgoLensTheme.BORDER_SUBTLE),
                BorderFactory.createEmptyBorder(12, 14, 10, 14)));

        titleLabel.setFont(AlgoLensTheme.FONT_TITLE);
        titleLabel.setForeground(AlgoLensTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);

        descriptionArea.setFont(AlgoLensTheme.FONT_SMALL);
        descriptionArea.setForeground(AlgoLensTheme.TEXT_SECONDARY);
        descriptionArea.setOpaque(false);
        descriptionArea.setEditable(false);
        descriptionArea.setFocusable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));
        descriptionArea.setAlignmentX(LEFT_ALIGNMENT);

        complexityBadgesPanel.setOpaque(false);
        complexityBadgesPanel.setAlignmentX(LEFT_ALIGNMENT);

        header.add(titleLabel);
        header.add(descriptionArea);
        header.add(complexityBadgesPanel);
        return header;
    }

    private JScrollPane buildCodeScrollPane() {
        codePane.setEditable(false);
        codePane.setBackground(AlgoLensTheme.BG_CANVAS);
        codePane.setForeground(AlgoLensTheme.TEXT_PRIMARY);
        codePane.setCaretColor(AlgoLensTheme.ACCENT_PRIMARY);
        codePane.setFont(AlgoLensTheme.FONT_MONO);
        codePane.setMargin(new Insets(10, 10, 10, 12));

        JScrollPane scrollPane = new JScrollPane(codePane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(AlgoLensTheme.BG_CANVAS);
        scrollPane.setRowHeaderView(gutter);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    /**
     * Populates the viewer with the selected algorithm's metadata and syntax-highlighted code lines.
     */
    public void setAlgorithm(AbstractAlgorithm algorithm) {
        AlgorithmMetadata meta = algorithm.getMetadata();
        titleLabel.setText(meta.displayName() + "  •  " + meta.category().getDisplayName());
        descriptionArea.setText(meta.description());

        complexityBadgesPanel.removeAll();
        complexityBadgesPanel.add(createBadge("Best", meta.bestTimeComplexity(), AlgoLensTheme.ACCENT_SUCCESS));
        complexityBadgesPanel.add(createBadge("Avg", meta.averageTimeComplexity(), AlgoLensTheme.ACCENT_PRIMARY));
        complexityBadgesPanel.add(createBadge("Worst", meta.worstTimeComplexity(), AlgoLensTheme.ACCENT_WARNING));
        complexityBadgesPanel.add(createBadge("Space", meta.spaceComplexity(), AlgoLensTheme.ACCENT_PIVOT));
        complexityBadgesPanel.revalidate();
        complexityBadgesPanel.repaint();

        loadCodeLines(algorithm.getCodeLines());
        setActiveLine(-1);
    }

    private void loadCodeLines(List<String> lines) {
        this.totalLines = lines.size();
        String joined = String.join("\n", lines);

        StyledDocument doc = codePane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
            SimpleAttributeSet baseAttrs = new SimpleAttributeSet();
            StyleConstants.setFontFamily(baseAttrs, AlgoLensTheme.FONT_MONO.getFamily());
            StyleConstants.setFontSize(baseAttrs, AlgoLensTheme.FONT_MONO.getSize());
            StyleConstants.setForeground(baseAttrs, AlgoLensTheme.TEXT_PRIMARY);
            doc.insertString(0, joined, baseAttrs);

            applySyntaxHighlighting(doc, joined);
        } catch (BadLocationException e) {
            throw new IllegalStateException("Failed to populate code document", e);
        }

        codePane.setCaretPosition(0);
        gutter.revalidate();
        gutter.repaint();
    }

    private void applySyntaxHighlighting(StyledDocument doc, String text) {
        SimpleAttributeSet keywordAttrs = colorAttrs(AlgoLensTheme.CODE_KEYWORD, true);
        SimpleAttributeSet callAttrs = colorAttrs(AlgoLensTheme.CODE_CALL, false);
        SimpleAttributeSet numberAttrs = colorAttrs(AlgoLensTheme.CODE_NUMBER, false);

        Matcher matcher = TOKEN_PATTERN.matcher(text);
        while (matcher.find()) {
            String token = matcher.group();
            int start = matcher.start();
            int len = matcher.end() - start;
            if (KEYWORDS.contains(token)) {
                doc.setCharacterAttributes(start, len, keywordAttrs, false);
            } else if (HELPER_CALLS.contains(token)) {
                doc.setCharacterAttributes(start, len, callAttrs, false);
            } else if (Character.isDigit(token.charAt(0))) {
                doc.setCharacterAttributes(start, len, numberAttrs, false);
            }
        }
    }

    /**
     * Highlights the given 1-based line number in both the code pane and line gutter,
     * scrolling it into view if necessary. Passing {@code -1} clears the active highlight.
     */
    public void setActiveLine(int oneBasedLine) {
        if (this.activeLine == oneBasedLine) {
            return;
        }
        this.activeLine = oneBasedLine;

        Highlighter highlighter = codePane.getHighlighter();
        if (activeHighlightTag != null) {
            highlighter.removeHighlight(activeHighlightTag);
            activeHighlightTag = null;
        }

        if (oneBasedLine >= 1 && oneBasedLine <= totalLines) {
            Document doc = codePane.getDocument();
            Element lineElement = doc.getDefaultRootElement().getElement(oneBasedLine - 1);
            if (lineElement != null) {
                int startOffset = lineElement.getStartOffset();
                int endOffset = Math.max(startOffset, lineElement.getEndOffset() - 1);
                if (startOffset == endOffset && lineElement.getEndOffset() <= doc.getLength()) {
                    endOffset = lineElement.getEndOffset();
                }
                try {
                    activeHighlightTag = highlighter.addHighlight(startOffset, endOffset, activeLinePainter);
                    Rectangle2D rect2d = codePane.modelToView2D(startOffset);
                    if (rect2d != null) {
                        Rectangle vis = codePane.getVisibleRect();
                        if (rect2d.getY() < vis.y || rect2d.getY() + rect2d.getHeight() > vis.y + vis.height) {
                            Rectangle target = new Rectangle(
                                    vis.x,
                                    Math.max(0, (int) rect2d.getY() - 24),
                                    Math.max(1, vis.width),
                                    (int) rect2d.getHeight() + 48);
                            codePane.scrollRectToVisible(target);
                        }
                    }
                } catch (BadLocationException ignored) {
                    // Ignore out-of-range offsets during rapid switching.
                }
            }
        }

        codePane.repaint();
        gutter.repaint();
    }

    private static SimpleAttributeSet colorAttrs(Color color, boolean bold) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, color);
        StyleConstants.setBold(attrs, bold);
        return attrs;
    }

    private static JPanel createBadge(String label, String value, Color accent) {
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        badge.setBackground(AlgoLensTheme.BG_ELEVATED);
        badge.setBorder(BorderFactory.createLineBorder(AlgoLensTheme.BORDER_SUBTLE, 1));

        JLabel keyLbl = new JLabel(label + ":");
        keyLbl.setFont(AlgoLensTheme.FONT_SMALL);
        keyLbl.setForeground(AlgoLensTheme.TEXT_MUTED);

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(AlgoLensTheme.FONT_SMALL_BOLD);
        valLbl.setForeground(accent);

        badge.add(keyLbl);
        badge.add(valLbl);
        return badge;
    }

    /**
     * Custom full-width debugger highlight painter for the active execution line.
     */
    private static final class ActiveLineHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {

        private ActiveLineHighlightPainter() {
            super(AlgoLensTheme.CODE_ACTIVE_BG);
        }

        @Override
        public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c, javax.swing.text.View view) {
            try {
                Rectangle2D r = c.modelToView2D(offs0);
                if (r == null) {
                    return null;
                }
                int y = (int) Math.floor(r.getY());
                int h = (int) Math.ceil(r.getHeight());
                int width = c.getWidth();

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(AlgoLensTheme.CODE_ACTIVE_BG);
                g2.fillRect(0, y, width, h);
                g2.setColor(AlgoLensTheme.CODE_ACTIVE_BORDER);
                g2.fillRect(0, y, 3, h);
                g2.dispose();
                return new Rectangle(0, y, width, h);
            } catch (BadLocationException e) {
                return null;
            }
        }
    }

    /**
     * Line number gutter synchronized with the {@link JTextPane} document lines.
     */
    private final class LineNumberGutter extends JPanel {

        private LineNumberGutter() {
            setBackground(AlgoLensTheme.BG_CODE_GUTTER);
            setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, AlgoLensTheme.BORDER_SUBTLE));
        }

        @Override
        public Dimension getPreferredSize() {
            int digits = Math.max(2, String.valueOf(Math.max(1, totalLines)).length());
            int width = 18 + digits * 8;
            return new Dimension(width, Math.max(codePane.getPreferredSize().height, 200));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(AlgoLensTheme.FONT_MONO);
                FontMetrics fm = g2.getFontMetrics();
                Element root = codePane.getDocument().getDefaultRootElement();
                int lineCount = root.getElementCount();
                int gutterWidth = getWidth();

                for (int i = 0; i < lineCount; i++) {
                    int lineNum = i + 1;
                    Element lineElem = root.getElement(i);
                    try {
                        Rectangle2D r = codePane.modelToView2D(lineElem.getStartOffset());
                        if (r == null) {
                            continue;
                        }
                        int y = (int) Math.floor(r.getY());
                        int h = (int) Math.ceil(r.getHeight());

                        if (lineNum == activeLine) {
                            g2.setColor(AlgoLensTheme.CODE_ACTIVE_BG);
                            g2.fillRect(0, y, gutterWidth, h);
                            g2.setColor(AlgoLensTheme.ACCENT_PRIMARY);
                            g2.setFont(AlgoLensTheme.FONT_MONO_BOLD);
                        } else {
                            g2.setColor(AlgoLensTheme.TEXT_MUTED);
                            g2.setFont(AlgoLensTheme.FONT_MONO);
                        }

                        String numText = String.valueOf(lineNum);
                        int textX = gutterWidth - fm.stringWidth(numText) - 8;
                        int textY = y + fm.getAscent() + Math.max(0, (h - fm.getHeight()) / 2);
                        g2.drawString(numText, textX, textY);
                    } catch (BadLocationException ignored) {
                        // Skip invalid location during updates.
                    }
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
