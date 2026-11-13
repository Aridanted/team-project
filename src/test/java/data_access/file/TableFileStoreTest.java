package data_access.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import use_case.table_file.TableFileException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TableFileStoreTest {

    @TempDir
    Path folder;

    private final TableFileStore store = new TableFileStore();

    @Test
    void writesThenReadsTheSameText() throws TableFileException {
        String path = folder.resolve("table.txt").toString();

        store.write(path, "inputs: A\noutputs: Y\n0 | 1\n");

        assertEquals("inputs: A\noutputs: Y\n0 | 1\n", store.read(path));
    }

    @Test
    void writingReplacesAnExistingFile() throws TableFileException {
        String path = folder.resolve("table.txt").toString();

        store.write(path, "old");
        store.write(path, "new");

        assertEquals("new", store.read(path));
    }

    @Test
    void missingFileIsAFriendlyError() {
        String path = folder.resolve("missing.txt").toString();

        TableFileException e = assertThrows(TableFileException.class, () -> store.read(path));
        assertEquals("Could not read the file " + path + ".", e.getMessage());
    }

    @Test
    void cannotWriteIntoAMissingFolder() {
        String path = folder.resolve("no/such/folder/table.txt").toString();

        assertThrows(TableFileException.class, () -> store.write(path, "x"));
    }

    @Test
    void rejectsHugeFiles() throws IOException {
        Path big = folder.resolve("big.txt");
        Files.write(big, new byte[(int) TableFileStore.MAX_FILE_BYTES + 1]);

        TableFileException e = assertThrows(TableFileException.class, () -> store.read(big.toString()));
        assertEquals("This file is too large to be a truth table.", e.getMessage());
    }
}
