package data_access.circ;

import entity.circuit.Circuit;
import use_case.save_built_circuit.CircuitFileWriteException;
import use_case.save_built_circuit.CircuitFileWriterInterface;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/** Writes a circuit as a positioned and wired Logisim-evolution file. */
public final class LogisimCircuitWriter implements CircuitFileWriterInterface {
    private final CircuitLayout layout;
    private final CircuitWireRouter router;
    private final CircuitXmlEncoder encoder;

    /**
     * Creates the writer with its layout, routing, and XML encoding steps.
     *
     * @param layout the component layout
     * @param router the wire router
     * @param encoder the Logisim XML encoder
     */
    public LogisimCircuitWriter(CircuitLayout layout, CircuitWireRouter router,
                                CircuitXmlEncoder encoder) {
        this.layout = Objects.requireNonNull(layout, "layout");
        this.router = Objects.requireNonNull(router, "router");
        this.encoder = Objects.requireNonNull(encoder, "encoder");
    }

    @Override
    public void write(Circuit circuit, String path) throws CircuitFileWriteException {
        if (path == null || path.isBlank()) {
            throw new CircuitFileWriteException("Choose where to save the circuit.", null);
        }

        try {
            Path target = Path.of(path).toAbsolutePath().normalize();
            if (target.getFileName() == null) {
                throw new IllegalArgumentException("Choose a file name.");
            }

            Circuit positioned = layout.layout(circuit);
            Circuit routed = router.route(positioned);
            String xml = encoder.encode(routed);
            save(target, xml);
        } catch (IOException | XMLStreamException | IllegalArgumentException error) {
            throw new CircuitFileWriteException(
                    "Could not save the circuit. " + error.getMessage(), error);
        }
    }

    private void save(Path target, String xml) throws IOException {
        Path temporary = Files.createTempFile(target.getParent(), ".logicoach-", ".tmp");

        try {
            Files.writeString(temporary, xml, StandardCharsets.UTF_8);
            replace(temporary, target);
        } catch (IOException error) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupError) {
                error.addSuppressed(cleanupError);
            }
            throw error;
        }
    }

    private void replace(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException error) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
