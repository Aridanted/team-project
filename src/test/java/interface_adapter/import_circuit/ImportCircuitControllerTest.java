package interface_adapter.import_circuit;

import org.junit.jupiter.api.Test;
import use_case.import_circuit.ImportCircuitInputBoundary;
import use_case.import_circuit.ImportCircuitInputData;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImportCircuitControllerTest {

    @Test
    void executePassesPathToInteractor() {
        RecordingInteractor interactor = new RecordingInteractor();
        ImportCircuitController controller = new ImportCircuitController(interactor);

        controller.execute("/tmp/demo.circ");

        assertEquals("/tmp/demo.circ", interactor.lastPath);
    }

    private static final class RecordingInteractor implements ImportCircuitInputBoundary {
        String lastPath;

        @Override
        public void execute(ImportCircuitInputData inputData) {
            lastPath = inputData.filePath();
        }
    }
}
