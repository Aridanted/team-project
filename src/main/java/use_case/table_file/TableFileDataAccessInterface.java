package use_case.table_file;

/** Reads and writes table text files. */
public interface TableFileDataAccessInterface {

    /** @throws TableFileException if the file cannot be read */
    String read(String path) throws TableFileException;

    /**
     * Creates or replaces the file.
     *
     * @throws TableFileException if the file cannot be written
     */
    void write(String path, String text) throws TableFileException;
}
