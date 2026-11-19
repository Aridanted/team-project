package view;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;

/** Stands in for a tab whose feature is not wired in yet. */
public final class PlaceholderView extends JPanel {

    private static final long serialVersionUID = 1L;

    public PlaceholderView(String message) {
        super(new BorderLayout());
        JLabel label = Styles.mutedLabel(message);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
    }
}
