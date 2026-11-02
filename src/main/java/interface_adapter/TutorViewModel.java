package interface_adapter;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class TutorViewModel {
    private String lastResponse = "";
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    public void setLastResponse(String lastResponse) {
        String old = this.lastResponse;
        this.lastResponse = lastResponse;
        support.firePropertyChange("response", old, lastResponse);
    }

    public String getLastResponse() {
        return lastResponse;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}