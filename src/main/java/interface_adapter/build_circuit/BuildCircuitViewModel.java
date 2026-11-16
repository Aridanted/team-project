package interface_adapter.build_circuit;

import interface_adapter.ViewModel;

/** Holds the Builder state and notifies listeners when it changes. */
public final class BuildCircuitViewModel extends ViewModel<BuildCircuitState> {

    /** Creates the Builder view model with an empty initial state. */
    public BuildCircuitViewModel() {
        super("build circuit", new BuildCircuitState("", "", ""));
    }
}
