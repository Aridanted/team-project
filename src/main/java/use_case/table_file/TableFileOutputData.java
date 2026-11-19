package use_case.table_file;

/**
 * @param path      the file that was saved or loaded
 * @param tableText the table text that was written or read
 */
public record TableFileOutputData(String path, String tableText) {
}
