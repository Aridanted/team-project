package interface_adapter.table_file;

/** The result of the latest save or load, for the truth table editor. */
public final class TableFileState {

    private String tableText = "";
    private String message = "";
    private String error = "";

    public TableFileState() {
    }

    public TableFileState(TableFileState copy) {
        tableText = copy.tableText;
        message = copy.message;
        error = copy.error;
    }

    /** @return the text of the table that was last loaded */
    public String getTableText() {
        return tableText;
    }

    public void setTableText(String tableText) {
        this.tableText = tableText;
    }

    /** @return for example "Saved half_adder.txt.", or "" */
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /** @return the error to show, or "" if there is none */
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
