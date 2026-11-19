package use_case.table_file;

import entity.truthtable.TruthTableFormatException;
import entity.truthtable.TruthTableTextFormat;

import java.util.Locale;
import java.util.Objects;

/**
 * Saves and loads expected truth tables. Only well-formed tables are saved, so every saved file
 * can be loaded and checked later.
 */
public final class TableFileInteractor implements TableFileInputBoundary {

    private final TableFileDataAccessInterface files;
    private final TruthTableTextFormat textFormat;
    private final TableFileOutputBoundary presenter;

    public TableFileInteractor(TableFileDataAccessInterface files, TruthTableTextFormat textFormat,
                               TableFileOutputBoundary presenter) {
        this.files = Objects.requireNonNull(files, "files");
        this.textFormat = Objects.requireNonNull(textFormat, "textFormat");
        this.presenter = Objects.requireNonNull(presenter, "presenter");
    }

    @Override
    public void save(SaveTableInputData inputData) {
        String text = inputData.tableText();
        if (isBlank(inputData.path())) {
            presenter.prepareFailView("Choose where to save the table.");
            return;
        }
        if (isBlank(text)) {
            presenter.prepareFailView("There is no table to save.");
            return;
        }
        try {
            textFormat.parse(text);
            String path = withTxtExtension(inputData.path());
            files.write(path, text);
            presenter.prepareSavedView(new TableFileOutputData(path, text));
        } catch (TruthTableFormatException e) {
            presenter.prepareFailView("Fix the table before saving (" + e.getMessage() + ").");
        } catch (TableFileException e) {
            presenter.prepareFailView(e.getMessage());
        }
    }

    @Override
    public void load(LoadTableInputData inputData) {
        if (isBlank(inputData.path())) {
            presenter.prepareFailView("Choose a table file to open.");
            return;
        }
        try {
            String text = files.read(inputData.path());
            presenter.prepareLoadedView(new TableFileOutputData(inputData.path(), text));
        } catch (TableFileException e) {
            presenter.prepareFailView(e.getMessage());
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String withTxtExtension(String path) {
        return path.toLowerCase(Locale.ROOT).endsWith(".txt") ? path : path + ".txt";
    }
}
