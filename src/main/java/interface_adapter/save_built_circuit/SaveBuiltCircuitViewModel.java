package interface_adapter.save_built_circuit;

import interface_adapter.ViewModel;

/** Holds save results and notifies the Builder when they change. */
public final class SaveBuiltCircuitViewModel extends ViewModel<SaveBuiltCircuitState> {

    /** Creates the save view model with no initial message or error. */
    public SaveBuiltCircuitViewModel() {
        super("save built circuit", new SaveBuiltCircuitState("", ""));
    }
}
