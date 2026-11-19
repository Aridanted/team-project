package use_case.table_file;

/** Thrown when a table file cannot be read or written. The message is shown to the user. */
public class TableFileException extends Exception {

    private static final long serialVersionUID = 1L;

    public TableFileException(String message) {
        super(message);
    }
}
