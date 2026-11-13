package interface_adapter.table_file;

import interface_adapter.ViewModel;

/**
 * View model for saving and loading tables. Fires {@link #TABLE_LOADED} when the editor should
 * show a newly loaded table, and the usual "state" for everything else.
 */
public final class TableFileViewModel extends ViewModel<TableFileState> {

    public static final String TABLE_LOADED = "tableLoaded";

    public TableFileViewModel() {
        super("table file", new TableFileState());
    }
}
