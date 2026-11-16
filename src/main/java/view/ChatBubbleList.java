package view;

import interface_adapter.tutor_chat.ChatBubble;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.text.View;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * The tutor conversation as chat bubbles: the student's on the right, the tutor's on the left.
 * A bubble is as wide as its text up to {@link #MAX_BUBBLE_SHARE} of the panel, then it wraps.
 * The text in a bubble can be selected and copied.
 */
final class ChatBubbleList extends JPanel implements Scrollable {

    private static final long serialVersionUID = 1L;

    static final double MAX_BUBBLE_SHARE = 0.8;
    static final Color STUDENT_FILL = new Color(219, 234, 254);
    static final Color STUDENT_TEXT = new Color(12, 68, 124);
    static final Color TUTOR_FILL = new Color(241, 241, 244);
    static final Color TUTOR_TEXT = new Color(33, 33, 33);
    private static final int SPACING = 6;
    private static final int SCROLL_STEP = 16;

    ChatBubbleList() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(Styles.padding());
        setBackground(Color.WHITE);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                revalidate();
            }
        });
    }

    /**
     * Shows {@code messages}, or {@code intro} as a muted note when there are none.
     *
     * @param messages the conversation, oldest first
     * @param intro    what to show in an empty conversation
     */
    void setMessages(List<ChatBubble> messages, String intro) {
        removeAll();
        if (messages.isEmpty()) {
            addRow(new Bubble(intro, getBackground(), Styles.MUTED, this::maxBubbleWidth), false);
        }
        for (ChatBubble message : messages) {
            boolean student = message.fromStudent();
            Bubble bubble = new Bubble(message.text(), student ? STUDENT_FILL : TUTOR_FILL,
                    student ? STUDENT_TEXT : TUTOR_TEXT, this::maxBubbleWidth);
            bubble.getAccessibleContext().setAccessibleName(message.sender());
            addRow(bubble, student);
        }
        revalidate();
        repaint();
    }

    private void addRow(Bubble bubble, boolean onRight) {
        if (getComponentCount() > 0) {
            add(Box.createVerticalStrut(SPACING));
        }
        JPanel row = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(bubble, onRight ? BorderLayout.EAST : BorderLayout.WEST);
        add(row);
    }

    /** @return the widest a bubble may be, given the panel's current width */
    int maxBubbleWidth() {
        Insets insets = getInsets();
        int width = getWidth() > 0 ? getWidth() : getParent() instanceof JViewport ? getParent().getWidth() : 0;
        int inner = width - insets.left - insets.right;
        return inner > 0 ? (int) (inner * MAX_BUBBLE_SHARE) : Bubble.FALLBACK_WIDTH;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
        return SCROLL_STEP;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? visible.height : visible.width;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    /** Fills the viewport when the conversation is short, so the white background reaches the bottom. */
    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof JViewport viewport && viewport.getHeight() > getPreferredSize().height;
    }

    /** One message: wrapped, selectable text on a rounded background. */
    static final class Bubble extends JTextArea {

        private static final long serialVersionUID = 1L;

        static final int FALLBACK_WIDTH = 320;
        private static final int MIN_WIDTH = 60;
        private static final int ARC = 14;
        /** Room for rounding in text measurement, so text that fits on one line is never wrapped. */
        private static final int SLACK = 4;

        private final Color fill;
        private final transient IntSupplier maxWidth;

        Bubble(String text, Color fill, Color foreground, IntSupplier maxWidth) {
            super(text);
            this.fill = fill;
            this.maxWidth = maxWidth;
            setEditable(false);
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(false);
            setForeground(foreground);
            setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        }

        /** As wide as the longest line, up to the limit; as tall as the text needs at that width. */
        @Override
        public Dimension getPreferredSize() {
            Insets insets = getInsets();
            int horizontal = insets.left + insets.right;
            int limit = Math.max(MIN_WIDTH, maxWidth.getAsInt());
            int width = Math.min(limit, naturalTextWidth() + horizontal);
            View root = getUI().getRootView(this);
            root.setSize(Math.max(1, width - horizontal), Integer.MAX_VALUE);
            int height = (int) Math.ceil(root.getPreferredSpan(View.Y_AXIS)) + insets.top + insets.bottom;
            return new Dimension(width, height);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        private int naturalTextWidth() {
            FontMetrics metrics = getFontMetrics(getFont());
            int widest = 0;
            for (String line : getText().split("\n", -1)) {
                widest = Math.max(widest, metrics.stringWidth(line));
            }
            return widest + SLACK;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(fill);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);
            } finally {
                g.dispose();
            }
            super.paintComponent(graphics);
        }
    }

    /** @return the bubbles in order, for tests */
    List<Bubble> bubbles() {
        return Arrays.stream(getComponents())
                .filter(JComponent.class::isInstance)
                .map(component -> ((JComponent) component).getComponents())
                .flatMap(Arrays::stream)
                .filter(Bubble.class::isInstance)
                .map(Bubble.class::cast)
                .toList();
    }
}
