package view;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Font;

/** Shared colours and fonts so every tab looks the same. */
final class Styles {

    static final Color ERROR = new Color(176, 0, 32);
    static final Color SUCCESS = new Color(0, 110, 50);
    static final Color MUTED = new Color(95, 99, 104);
    static final Color PASS_ROW = new Color(232, 245, 233);
    static final Color FAIL_ROW = new Color(253, 236, 234);
    static final Font MONOSPACED = new Font(Font.MONOSPACED, Font.PLAIN, 13);
    static final int GAP = 8;

    private Styles() {
    }

    /** @return a label for inline errors; empty text hides nothing, so layouts do not jump */
    static JLabel errorLabel() {
        JLabel label = new JLabel(" ");
        label.setForeground(ERROR);
        return label;
    }

    static JLabel mutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        return label;
    }

    static Border padding() {
        return BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP);
    }

    static Border titled(String title) {
        return BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    /** Shows {@code text}, or a blank that keeps the label's height when there is nothing to say. */
    static void setMessage(JLabel label, String text) {
        label.setText(text == null || text.isEmpty() ? " " : text);
    }
}
