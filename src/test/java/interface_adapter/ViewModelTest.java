package interface_adapter;

import org.junit.jupiter.api.Test;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ViewModelTest {

    @Test
    void firePropertyChangedNotifiesListenersWithState() {
        ViewModel<String> viewModel = new ViewModel<>("demo", "initial");
        List<PropertyChangeEvent> events = new ArrayList<>();
        viewModel.addPropertyChangeListener(events::add);

        viewModel.setState("updated");
        viewModel.firePropertyChanged();

        assertEquals(1, events.size());
        assertEquals("state", events.get(0).getPropertyName());
        assertSame("updated", events.get(0).getNewValue());
    }
}
