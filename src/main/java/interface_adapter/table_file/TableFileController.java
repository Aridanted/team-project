package interface_adapter.table_file;

import use_case.table_file.LoadTableInputData;
import use_case.table_file.SaveTableInputData;
import use_case.table_file.TableFileInputBoundary;

import java.util.Objects;

/** Called by the truth table editor's "Save table..." and "Load table..." buttons. */
public final class TableFileController {

    private final TableFileInputBoundary interactor;

    public TableFileController(TableFileInputBoundary interactor) {
        this.interactor = Objects.requireNonNull(interactor, "interactor");
    }

    public void save(String path, String tableText) {
        interactor.save(new SaveTableInputData(path, tableText));
    }

    public void load(String path) {
        interactor.load(new LoadTableInputData(path));
    }
}
