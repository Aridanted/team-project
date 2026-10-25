package interface_adapter;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;

/**
 * Base view model that notifies listeners when {@link #getState()} changes.
 *
 * @param <T> the type of state this view model holds
 */
public class ViewModel<T> {

    private final String viewName;
    private T state;
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    /**
     * @param viewName     identifier for this screen, shown in navigation
     * @param initialState starting state before any user action
     */
    public ViewModel(String viewName, T initialState) {
        this.viewName = Objects.requireNonNull(viewName, "viewName");
        this.state = Objects.requireNonNull(initialState, "initialState");
    }

    public String getViewName() {
        return viewName;
    }

    public T getState() {
        return state;
    }

    public void setState(T state) {
        this.state = Objects.requireNonNull(state, "state");
    }

    /** Fires a property change for {@code state} with the current state as the new value. */
    public void firePropertyChanged() {
        firePropertyChanged("state");
    }

    /**
     * @param propertyName name of the property that changed
     */
    public void firePropertyChanged(String propertyName) {
        support.firePropertyChange(propertyName, null, getState());
    }

    /**
     * @param listener called when a property on this view model changes
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
