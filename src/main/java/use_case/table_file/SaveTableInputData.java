package use_case.table_file;

/**
 * @param path      where to save; ".txt" is added if missing
 * @param tableText the expected truth table as typed
 */
public record SaveTableInputData(String path, String tableText) {
}
