package interface_adapter.import_circuit;

import interface_adapter.ViewModel;

/** View model for the import-circuit screen. */
public class ImportCircuitViewModel extends ViewModel<ImportCircuitState> {

    public ImportCircuitViewModel() {
        super("import circuit", new ImportCircuitState());
    }
}
