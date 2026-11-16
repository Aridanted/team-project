package view;

import javax.swing.JSplitPane;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/** Small layout helpers shared by the views. */
final class Layouts {

    private Layouts() {
    }

    /**
     * Puts the divider at {@code share} of the split pane once it first gets a size. A split pane
     * ignores a proportional location until it is on screen, so setting it in a constructor does
     * nothing; after that first time the student's own dragging is left alone.
     */
    static void placeDividerOnFirstShow(JSplitPane split, double share) {
        split.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                if (split.getWidth() > 0 && split.getHeight() > 0) {
                    split.setDividerLocation(share);
                    split.removeComponentListener(this);
                }
            }
        });
    }
}
