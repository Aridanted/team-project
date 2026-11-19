package use_case.table_file;

/** Receives the outcome of saving or loading a table file. */
public interface TableFileOutputBoundary {

    void prepareSavedView(TableFileOutputData outputData);

    void prepareLoadedView(TableFileOutputData outputData);

    /** @param errorMessage why nothing was saved or loaded, ready to show to the user */
    void prepareFailView(String errorMessage);
}
