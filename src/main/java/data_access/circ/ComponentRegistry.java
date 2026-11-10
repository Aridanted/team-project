package data_access.circ;

import entity.circuit.GateType;
import entity.circuit.Orientation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Maps a Logisim component, identified by library description and name, to its
 * {@link ComponentDefinition} (Factory). Anything not registered is unsupported.
 */
final class ComponentRegistry {

    static final String BASE = "#Base";
    static final String WIRING = "#Wiring";
    static final String GATES = "#Gates";
    /** Library description of subcircuits, which have no {@code lib} attribute. */
    static final String SUBCIRCUIT = "";

    record Key(String library, String name) {
    }

    private final Map<Key, ComponentDefinition> definitions;
    private final Set<Key> ignored;
    private final Map<Key, String> unsupportedReasons;
    private final Map<String, String> unsupportedReasonsByLibrary;

    /**
     * @param definitions                 supported components
     * @param ignored                     components with no effect on logic, dropped silently
     * @param unsupportedReasons          advice for specific unsupported components
     * @param unsupportedReasonsByLibrary advice for any other unsupported component of a library
     */
    ComponentRegistry(Map<Key, ComponentDefinition> definitions, Set<Key> ignored,
                      Map<Key, String> unsupportedReasons, Map<String, String> unsupportedReasonsByLibrary) {
        this.definitions = Map.copyOf(definitions);
        this.ignored = Set.copyOf(ignored);
        this.unsupportedReasons = Map.copyOf(unsupportedReasons);
        this.unsupportedReasonsByLibrary = Map.copyOf(unsupportedReasonsByLibrary);
    }

    /** The components of Logisim-evolution that LogiCoach supports. */
    static ComponentRegistry logisimEvolution() {
        Map<Key, ComponentDefinition> definitions = new HashMap<>();
        definitions.put(new Key(WIRING, "Pin"), new PinDefinition());
        definitions.put(new Key(WIRING, "Constant"), ConstantDefinition.fromValueAttribute());
        definitions.put(new Key(WIRING, "Power"), ConstantDefinition.fixed(true, Orientation.NORTH));
        definitions.put(new Key(WIRING, "Ground"), ConstantDefinition.fixed(false, Orientation.SOUTH));
        definitions.put(new Key(GATES, "AND Gate"), GateDefinition.standard(GateType.AND, 0));
        definitions.put(new Key(GATES, "OR Gate"), GateDefinition.standard(GateType.OR, 0));
        definitions.put(new Key(GATES, "NAND Gate"), GateDefinition.standard(GateType.NAND, 10));
        definitions.put(new Key(GATES, "NOR Gate"), GateDefinition.standard(GateType.NOR, 10));
        definitions.put(new Key(GATES, "XOR Gate"), GateDefinition.xorFamily(GateType.XOR, GateType.XOR_ONE, 10));
        definitions.put(new Key(GATES, "XNOR Gate"), GateDefinition.xorFamily(GateType.XNOR, GateType.XNOR_ONE, 20));
        definitions.put(new Key(GATES, "NOT Gate"), new UnaryGateDefinition(GateType.NOT, 30, true));
        definitions.put(new Key(GATES, "Buffer"), new UnaryGateDefinition(GateType.BUFFER, 20, false));

        Set<Key> ignored = Set.of(
                new Key(BASE, "Text"),
                new Key(WIRING, "Probe"),
                new Key(WIRING, "NoConnect"));

        Map<Key, String> reasons = Map.of(
                new Key(WIRING, "Tunnel"), "Connect the signals with wires instead.",
                new Key(WIRING, "Splitter"), "Multi-bit wiring is not supported; use 1-bit pins.",
                new Key(WIRING, "Clock"), "Sequential circuits are not supported.");

        Map<String, String> reasonsByLibrary = Map.of(
                SUBCIRCUIT, "Subcircuits are not supported; build the whole design in the main circuit.",
                GATES, "Build it from AND, OR, NOT, NAND, NOR, XOR or XNOR gates instead.",
                "#Plexers", "Build it from basic gates instead.",
                "#Arithmetic", "Build it from basic gates instead.",
                "#Memory", "Sequential circuits are not supported.",
                "#I/O", "Use Input/Output pins instead.");

        return new ComponentRegistry(definitions, ignored, reasons, reasonsByLibrary);
    }

    /**
     * @return the definition for the component, an {@link UnsupportedDefinition} if it is not
     *         supported, or empty if it should be ignored
     */
    Optional<ComponentDefinition> definitionFor(RawComponent raw) {
        Key key = new Key(raw.library(), raw.name());
        if (ignored.contains(key)) {
            return Optional.empty();
        }
        ComponentDefinition definition = definitions.get(key);
        if (definition != null) {
            return Optional.of(definition);
        }
        String reason = unsupportedReasons.getOrDefault(key,
                unsupportedReasonsByLibrary.getOrDefault(raw.library(), ""));
        return Optional.of(new UnsupportedDefinition(reason));
    }
}
