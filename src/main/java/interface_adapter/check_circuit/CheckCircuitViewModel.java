package interface_adapter.check_circuit;

import interface_adapter.ViewModel;

/** View model for the check screen. */
public final class CheckCircuitViewModel extends ViewModel<CheckCircuitState> {

    public CheckCircuitViewModel() {
        super("check circuit", new CheckCircuitState());
    }
}
