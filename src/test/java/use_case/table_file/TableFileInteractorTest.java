package use_case.table_file;

import entity.truthtable.TruthTableTextFormat;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableFileInteractorTest {

    private static final String TABLE = "inputs: A\noutputs: Y\n0 | 1\n1 | 0\n";

    private static final class FakeFiles implements TableFileDataAccessInterface {
        private final Map<String, String> files = new HashMap<>();
        private boolean broken;

        @Override
        public String read(String path) throws TableFileException {
            if (broken || !files.containsKey(path)) {
                throw new TableFileException("Could not read the file " + path + ".");
            }
            return files.get(path);
        }

        @Override
        public void write(String path, String text) throws TableFileException {
            if (broken) {
                throw new TableFileException("Could not save to " + path + ".");
            }
            files.put(path, text);
        }
    }

    private static final class CapturingPresenter implements TableFileOutputBoundary {
        private TableFileOutputData saved;
        private TableFileOutputData loaded;
        private String error;

        @Override
        public void prepareSavedView(TableFileOutputData outputData) {
            saved = outputData;
        }

        @Override
        public void prepareLoadedView(TableFileOutputData outputData) {
            loaded = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            error = errorMessage;
        }
    }

    private final FakeFiles files = new FakeFiles();
    private final CapturingPresenter presenter = new CapturingPresenter();
    private final TableFileInteractor interactor =
            new TableFileInteractor(files, new TruthTableTextFormat(), presenter);

    @Test
    void savesAndLoadsTheSameText() {
        interactor.save(new SaveTableInputData("not.txt", TABLE));
        interactor.load(new LoadTableInputData("not.txt"));

        assertNull(presenter.error);
        assertEquals("not.txt", presenter.saved.path());
        assertEquals(TABLE, presenter.loaded.tableText());
    }

    @Test
    void addsTxtExtensionWhenMissing() {
        interactor.save(new SaveTableInputData("half_adder", TABLE));

        assertEquals("half_adder.txt", presenter.saved.path());
        assertTrue(files.files.containsKey("half_adder.txt"));
    }

    @Test
    void refusesToSaveAMalformedTable() {
        interactor.save(new SaveTableInputData("bad.txt", "inputs: A\noutputs: Y\n0 | \n"));

        assertEquals("Fix the table before saving (line 3: expected 1 output values, found 0).", presenter.error);
        assertTrue(files.files.isEmpty());
    }

    @Test
    void needsAPathAndATable() {
        interactor.save(new SaveTableInputData(" ", TABLE));
        assertEquals("Choose where to save the table.", presenter.error);

        interactor.save(new SaveTableInputData("t.txt", ""));
        assertEquals("There is no table to save.", presenter.error);

        interactor.load(new LoadTableInputData(""));
        assertEquals("Choose a table file to open.", presenter.error);
    }

    @Test
    void reportsFileErrors() {
        files.broken = true;

        interactor.save(new SaveTableInputData("t.txt", TABLE));
        assertEquals("Could not save to t.txt.", presenter.error);

        interactor.load(new LoadTableInputData("t.txt"));
        assertEquals("Could not read the file t.txt.", presenter.error);
    }
}
