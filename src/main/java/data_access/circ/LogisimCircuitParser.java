package data_access.circ;

import data_access.circ.ConnectivityChecker.Placement;
import entity.circuit.Circuit;
import entity.circuit.CircuitComponent;
import entity.circuit.CircuitWarning;
import entity.circuit.Point;
import entity.circuit.UnsupportedComponent;
import entity.circuit.Wire;
import use_case.import_circuit.CircuitFileDataAccessInterface;
import use_case.import_circuit.CircuitParseException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reads Logisim-evolution .circ files into {@link Circuit}s. Coordinates the steps; each step
 * lives in its own class:
 * <ol>
 *   <li>{@link CircXmlReader}: XML to raw components and wires;</li>
 *   <li>{@link ComponentRegistry} and its {@link ComponentDefinition}s: component meaning and
 *       port positions;</li>
 *   <li>{@link NetBuilder}: which ports and wires are connected;</li>
 *   <li>{@link PinLabeler}: unique, non-empty pin labels;</li>
 *   <li>{@link ConnectivityChecker}: wiring errors and warnings.</li>
 * </ol>
 */
public final class LogisimCircuitParser implements CircuitFileDataAccessInterface {

    static final long MAX_FILE_BYTES = 5L * 1024 * 1024;

    private final CircXmlReader xmlReader;
    private final ComponentRegistry registry;
    private final NetBuilder netBuilder;
    private final PinLabeler pinLabeler;
    private final ConnectivityChecker connectivityChecker;

    public LogisimCircuitParser() {
        this(new CircXmlReader(), ComponentRegistry.logisimEvolution(), new NetBuilder(),
                new PinLabeler(), new ConnectivityChecker());
    }

    LogisimCircuitParser(CircXmlReader xmlReader, ComponentRegistry registry, NetBuilder netBuilder,
                         PinLabeler pinLabeler, ConnectivityChecker connectivityChecker) {
        this.xmlReader = xmlReader;
        this.registry = registry;
        this.netBuilder = netBuilder;
        this.pinLabeler = pinLabeler;
        this.connectivityChecker = connectivityChecker;
    }

    @Override
    public Circuit load(String path) throws CircuitParseException {
        try {
            Path file = Path.of(path);
            if (Files.size(file) > MAX_FILE_BYTES) {
                throw Diagnostics.fileTooLarge(MAX_FILE_BYTES);
            }
            try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
                return parse(in);
            }
        } catch (IOException | InvalidPathException e) {
            throw Diagnostics.unreadableFile(path);
        }
    }

    /** Parses .circ XML from a stream. The caller closes the stream. */
    public Circuit parse(InputStream xml) throws CircuitParseException {
        RawCircuit raw = xmlReader.read(xml);

        List<PlacedComponent> placed = new ArrayList<>();
        for (RawComponent component : raw.components()) {
            Optional<ComponentDefinition> definition = registry.definitionFor(component);
            if (definition.isPresent()) {
                placed.add(definition.get().place(component));
            }
        }

        List<Point> ports = placed.stream().flatMap(p -> p.ports().stream()).toList();
        NetMap nets = netBuilder.build(raw.wires(), ports);

        List<CircuitComponent> components = new ArrayList<>();
        for (int i = 0; i < placed.size(); i++) {
            components.add(placed.get(i).factory().create("c" + i, nets));
        }

        List<CircuitWarning> warnings = new ArrayList<>();
        for (CircuitComponent component : components) {
            if (component instanceof UnsupportedComponent unsupported) {
                warnings.add(Diagnostics.unsupported(unsupported.name(), unsupported.location(),
                        unsupported.reason()));
            }
        }
        components = pinLabeler.assignLabels(components, warnings);

        List<Placement> placements = new ArrayList<>();
        for (int i = 0; i < components.size(); i++) {
            placements.add(new Placement(components.get(i), placed.get(i).inputPorts()));
        }
        connectivityChecker.check(placements, nets, warnings);

        List<Wire> wires = raw.wires().stream()
                .map(w -> new Wire(w.from(), w.to(), nets.netAt(w.from())))
                .toList();
        return new Circuit(raw.name(), components, wires, nets.netCount(), warnings);
    }
}
