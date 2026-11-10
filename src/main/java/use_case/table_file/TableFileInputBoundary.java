package use_case.table_file;

/** Saves the expected truth table to a text file, or loads one back. */
public interface TableFileInputBoundary {

    void save(SaveTableInputData inputData);

    void load(LoadTableInputData inputData);
}
