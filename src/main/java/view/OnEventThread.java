package view;

import javax.swing.SwingUtilities;
import java.beans.PropertyChangeListener;

/**
 * Presenters may fire view model changes from background threads (AI calls, file reads). Swing
 * may only be touched on the event thread, so views register their listeners through this.
 */
final class OnEventThread {

    private OnEventThread() {
    }

    static PropertyChangeListener listener(PropertyChangeListener listener) {
        return event -> {
            if (SwingUtilities.isEventDispatchThread()) {
                listener.propertyChange(event);
            } else {
                SwingUtilities.invokeLater(() -> listener.propertyChange(event));
            }
        };
    }
}
