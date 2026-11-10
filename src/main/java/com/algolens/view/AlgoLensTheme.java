package com.algolens.view;

import com.algolens.model.ElementState;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.Border;

/**
 * Centralized visual styling tokens, color palette, typography, and FlatLaf-aware
 * Swing component helpers for AlgoLens.
 */
public final class AlgoLensTheme {

    private AlgoLensTheme() {
    }

    // Surfaces & Borders
    public static final Color BG_WINDOW = new Color(11, 17, 32);
    public static final Color BG_SURFACE = new Color(17, 24, 39);
    public static final Color BG_ELEVATED = new Color(30, 41, 59);
    public static final Color BG_CANVAS = new Color(15, 23, 42);
    public static final Color BG_CODE_GUTTER = new Color(13, 20, 36);
    public static final Color BORDER_SUBTLE = new Color(41, 53, 72);
    public static final Color GRID_LINE = new Color(30, 41, 59);

    // Typography Colors
    public static final Color TEXT_PRIMARY = new Color(241, 245, 249);
    public static final Color TEXT_SECONDARY = new Color(148, 163, 184);
    public static final Color TEXT_MUTED = new Color(100, 116, 139);

    // Semantic Accents
    public static final Color ACCENT_PRIMARY = new Color(56, 189, 248);
    public static final Color ACCENT_PRIMARY_DARK = new Color(14, 165, 233);
    public static final Color ACCENT_SUCCESS = new Color(16, 185, 129);
    public static final Color ACCENT_WARNING = new Color(245, 158, 11);
    public static final Color ACCENT_DANGER = new Color(239, 68, 68);
    public static final Color ACCENT_PIVOT = new Color(168, 85, 247);

    // Element Bar Colors
    public static final Color BAR_DEFAULT = new Color(59, 130, 246);
    public static final Color BAR_COMPARING = ACCENT_WARNING;
    public static final Color BAR_SWAPPING = ACCENT_DANGER;
    public static final Color BAR_PIVOT = ACCENT_PIVOT;
    public static final Color BAR_SORTED = ACCENT_SUCCESS;
    public static final Color BAR_TARGET = ACCENT_PRIMARY;
    public static final Color BAR_DISCARDED = new Color(51, 65, 85);
    public static final Color BAR_FOUND = ACCENT_SUCCESS;

    // Code Editor Colors
    public static final Color CODE_ACTIVE_BG = new Color(56, 189, 248, 42);
    public static final Color CODE_ACTIVE_BORDER = ACCENT_PRIMARY;
    public static final Color CODE_KEYWORD = new Color(192, 132, 252);
    public static final Color CODE_TYPE = new Color(56, 189, 248);
    public static final Color CODE_CALL = new Color(96, 165, 250);
    public static final Color CODE_NUMBER = new Color(251, 191, 36);

    // Fonts
    public static final Font FONT_UI = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_UI_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_SMALL_BOLD = new Font("Segoe UI", Font.BOLD, 11);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 13);
    public static final Font FONT_MONO_BOLD = new Font("Consolas", Font.BOLD, 13);

    public static Color colorForState(ElementState state) {
        if (state == null) {
            return BAR_DEFAULT;
        }
        return switch (state) {
            case DEFAULT -> BAR_DEFAULT;
            case COMPARING -> BAR_COMPARING;
            case SWAPPING -> BAR_SWAPPING;
            case PIVOT -> BAR_PIVOT;
            case SORTED -> BAR_SORTED;
            case TARGET -> BAR_TARGET;
            case DISCARDED -> BAR_DISCARDED;
            case FOUND -> BAR_FOUND;
        };
    }

    public static Border createCardBorder() {
        return BorderFactory.createLineBorder(BORDER_SUBTLE, 1);
    }

    public static JButton createButton(String text, Color baseBg, Color hoverBg, Color foreground) {
        JButton button = new JButton(text);
        button.setFont(FONT_UI_BOLD);
        button.setForeground(foreground);
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty(
                FlatClientProperties.STYLE,
                String.format(
                        "arc: 10; margin: 6,14,6,14; borderWidth: 1;"
                                + "background: %s; hoverBackground: %s; pressedBackground: %s;"
                                + "borderColor: %s; disabledBackground: %s; disabledText: %s",
                        toHex(baseBg),
                        toHex(hoverBg),
                        toHex(hoverBg.darker()),
                        toHex(BORDER_SUBTLE),
                        toHex(BG_ELEVATED),
                        toHex(TEXT_MUTED)));
        return button;
    }

    public static void styleTextField(JTextField field, String placeholderText) {
        field.setFont(FONT_MONO);
        field.setForeground(TEXT_PRIMARY);
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholderText);
        field.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        field.putClientProperty(
                FlatClientProperties.STYLE,
                String.format(
                        "arc: 10; margin: 5,10,5,10; background: %s; borderColor: %s; focusedBorderColor: %s",
                        toHex(BG_ELEVATED),
                        toHex(BORDER_SUBTLE),
                        toHex(ACCENT_PRIMARY)));
    }

    public static void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(FONT_UI);
        comboBox.setForeground(TEXT_PRIMARY);
        comboBox.setFocusable(false);
        comboBox.putClientProperty(
                FlatClientProperties.STYLE,
                String.format(
                        "arc: 10; padding: 4,8,4,8; background: %s; borderColor: %s;"
                                + "buttonBackground: %s; buttonArrowColor: %s; buttonHoverArrowColor: %s;"
                                + "popupBackground: %s; selectionArc: 6",
                        toHex(BG_ELEVATED),
                        toHex(BORDER_SUBTLE),
                        toHex(BG_ELEVATED),
                        toHex(TEXT_SECONDARY),
                        toHex(ACCENT_PRIMARY),
                        toHex(BG_ELEVATED)));
    }

    public static void styleSlider(JSlider slider) {
        slider.setOpaque(false);
        slider.setFocusable(false);
        slider.putClientProperty(
                FlatClientProperties.STYLE,
                String.format(
                        "trackColor: %s; trackValueColor: %s; thumbColor: %s; hoverThumbColor: %s; focusedColor: %s",
                        toHex(BG_ELEVATED),
                        toHex(ACCENT_PRIMARY),
                        toHex(ACCENT_PRIMARY),
                        toHex(ACCENT_PRIMARY_DARK),
                        toHex(ACCENT_PRIMARY)));
    }

    public static String toHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
