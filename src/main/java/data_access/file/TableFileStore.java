package data_access.file;

import use_case.table_file.TableFileDataAccessInterface;
import use_case.table_file.TableFileException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/** Stores expected truth tables as UTF-8 text files on disk. */
public final class TableFileStore implements TableFileDataAccessInterface {

    static final long MAX_FILE_BYTES = 1024 * 1024;

    @Override
    public String read(String path) throws TableFileException {
        try {
            Path file = Path.of(path);
            if (Files.size(file) > MAX_FILE_BYTES) {
                throw new TableFileException("This file is too large to be a truth table.");
            }
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException | InvalidPathException e) {
            throw new TableFileException("Could not read the file " + path + ".");
        }
    }

    @Override
    public void write(String path, String text) throws TableFileException {
        try {
            Files.writeString(Path.of(path), text, StandardCharsets.UTF_8);
        } catch (IOException | InvalidPathException e) {
            throw new TableFileException("Could not save to " + path + ".");
        }
    }
}
