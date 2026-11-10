package data_access.circ;

import entity.circuit.Circuit;
import entity.circuit.CircuitWarning;
import entity.circuit.Gate;
import entity.circuit.GateInput;
import entity.circuit.GateType;
import entity.circuit.InputPin;
import entity.circuit.OutputPin;
import entity.circuit.Pin;
import entity.circuit.Point;
import entity.circuit.UnsupportedComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import use_case.import_circuit.CircuitParseException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogisimCircuitParserTest {

    private final LogisimCircuitParser parser = new LogisimCircuitParser();

    static Circuit parseFixture(String name) throws CircuitParseException {
        try (InputStream in = LogisimCircuitParserTest.class.getResourceAsStream("/circuits/" + name + ".circ")) {
            if (in == null) {
                throw new IllegalArgumentException("missing fixture " + name);
            }
            return new LogisimCircuitParser().parse(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Circuit parseXml(String xml) throws CircuitParseException {
        return parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static String circuit(String body) {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="no"?>
                <project source="3.7.2" version="1.0">
                  <lib desc="#Wiring" name="0"/>
                  <lib desc="#Gates" name="1"/>
                  <main name="main"/>
                  <circuit name="main">
                %s
                  </circuit>
                </project>
                """.formatted(body);
    }

    private static String inputPin(int x, int y, String label) {
        return "<comp lib=\"0\" loc=\"(%d,%d)\" name=\"Pin\"><a name=\"label\" val=\"%s\"/></comp>"
                .formatted(x, y, label);
    }

    private static Map<String, Pin> pinsByLabel(Circuit circuit) {
        return circuit.components().stream()
                .filter(Pin.class::isInstance).map(Pin.class::cast)
                .collect(Collectors.toMap(Pin::label, Function.identity()));
    }

    private static List<String> codes(Circuit circuit) {
        return circuit.warnings().stream().map(CircuitWarning::code).toList();
    }

    private static List<String> messages(Circuit circuit) {
        return circuit.warnings().stream().map(CircuitWarning::message).toList();
    }

    @Test
    void parsesHalfAdderStructure() throws CircuitParseException {
        Circuit circuit = parseFixture("half_adder");

        assertEquals("main", circuit.name());
        assertEquals(List.of("A", "B"), circuit.inputPins().stream().map(InputPin::label).toList());
        assertEquals(List.of("Cout", "Sum"), circuit.outputPins().stream().map(OutputPin::label).toList());
        assertEquals(List.of(), circuit.warnings());
        assertTrue(circuit.isSimulatable());

        Map<String, Pin> pins = pinsByLabel(circuit);
        int a = pins.get("A").net();
        int b = pins.get("B").net();
        assertNotEquals(a, b, "crossing wires must not connect");

        Gate xor = circuit.gates().stream().filter(g -> g.type() == GateType.XOR).findFirst().orElseThrow();
        Gate and = circuit.gates().stream().filter(g -> g.type() == GateType.AND).findFirst().orElseThrow();
        assertEquals(List.of(a, b), xor.inputNets());
        assertEquals(List.of(b, a), and.inputNets());
        assertEquals(xor.outputNet(), pins.get("Sum").net());
        assertEquals(and.outputNet(), pins.get("Cout").net());
        assertEquals(new Point(300, 120), xor.location());
        assertFalse(circuit.wires().isEmpty());
    }

    @Test
    void keepsUnsupportedComponentsAndIgnoresDecorations() throws CircuitParseException {
        Circuit circuit = parseFixture("unsupported");

        assertFalse(circuit.isSimulatable());
        assertEquals(Set.of("D Flip-Flop", "Multiplexer", "LED", "Tunnel", "Clock", "helper"),
                circuit.unsupportedComponents().stream().map(UnsupportedComponent::name).collect(Collectors.toSet()));
        assertEquals(List.of("W101", "W101", "W101", "W101", "W101", "W101"), codes(circuit));
        assertTrue(messages(circuit).contains("Unsupported component: LED at (600,300). Simulation is disabled. "
                + "Use Input/Output pins instead."));
        assertTrue(messages(circuit).stream().anyMatch(m -> m.contains("Subcircuits are not supported")));
        assertEquals(1, circuit.gates().size());
    }

    @Test
    void warnsAboutFloatingInputsAndNearMisses() throws CircuitParseException {
        Circuit circuit = parseFixture("floating");

        assertEquals(Set.of(
                "Input 2 of the AND gate at (300,100) is not connected (treated as unknown).",
                "Output pin Z at (350,200) is not connected (treated as unknown).",
                "A wire ends near (240,120) but nothing is attached."), Set.copyOf(messages(circuit)));
        assertEquals(3, circuit.warnings().size());
    }

    @Test
    void namesUnlabelledPinsInReadingOrderSkippingTakenNames() throws CircuitParseException {
        Circuit circuit = parseFixture("unlabelled_pins");

        assertEquals(List.of("IN1", "IN2", "IN3"), circuit.inputPins().stream().map(InputPin::label).toList());
        Map<String, Pin> pins = pinsByLabel(circuit);
        assertEquals(new Point(100, 100), pins.get("IN2").location());
        assertEquals(new Point(100, 200), pins.get("IN3").location());
        assertEquals(new Point(400, 200), pins.get("OUT1").location());
        assertEquals(List.of("W103", "W103", "W103"), codes(circuit));
        assertEquals("A pin at (100,100) has no label; using IN2.", circuit.warnings().get(0).message());
    }

    @Test
    void rejectsDuplicateLabelsIgnoringCase() {
        CircuitParseException e = assertThrows(CircuitParseException.class,
                () -> parseFixture("duplicate_labels"));
        assertEquals("E003", e.code());
        assertEquals("Two pins are both labelled \"a\" (at (100,100) and (100,200)). "
                + "Give each pin a unique label.", e.getMessage());
    }

    @Test
    void rejectsTwoDriversOnOneNet() {
        CircuitParseException e = assertThrows(CircuitParseException.class, () -> parseFixture("contention"));
        assertEquals("E004", e.code());
        assertEquals("Two outputs are connected together near (100,200).", e.getMessage());
    }

    @Test
    void rejectsMultiBitComponents() {
        CircuitParseException e = assertThrows(CircuitParseException.class, () -> parseFixture("multibit"));
        assertEquals("E005", e.code());
    }

    @Test
    void rejectsMalformedXml() {
        CircuitParseException e = assertThrows(CircuitParseException.class,
                () -> parseXml("<?xml version=\"1.0\"?><project><circuit name=\"main\">"));
        assertEquals("E001", e.code());
        assertEquals("This is not a valid Logisim circuit file.", e.getMessage());
    }

    @Test
    void rejectsXmlThatIsNotAProject() {
        CircuitParseException e = assertThrows(CircuitParseException.class,
                () -> parseXml("<?xml version=\"1.0\"?><html><body/></html>"));
        assertEquals("E001", e.code());
    }

    @Test
    void rejectsDoctypeToPreventExternalEntityAttacks() {
        String xxe = """
                <?xml version="1.0"?>
                <!DOCTYPE project [<!ENTITY secret SYSTEM "file:///etc/passwd">]>
                <project><circuit name="&secret;"/></project>
                """;
        CircuitParseException e = assertThrows(CircuitParseException.class, () -> parseXml(xxe));
        assertEquals("E001", e.code());
    }

    @Test
    void rejectsFileWithoutCircuit() {
        CircuitParseException e = assertThrows(CircuitParseException.class,
                () -> parseXml("<?xml version=\"1.0\"?><project><lib desc=\"#Wiring\" name=\"0\"/></project>"));
        assertEquals("E002", e.code());
    }

    @Test
    void rejectsUnknownLibraryReference() {
        CircuitParseException e = assertThrows(CircuitParseException.class,
                () -> parseXml(circuit("<comp lib=\"9\" loc=\"(10,10)\" name=\"Pin\"/>")));
        assertEquals("E001", e.code());
    }

    @Test
    void rejectsMalformedLocation() {
        CircuitParseException e = assertThrows(CircuitParseException.class,
                () -> parseXml(circuit("<comp lib=\"0\" loc=\"10,10\" name=\"Pin\"/>")));
        assertEquals("E001", e.code());
    }

    @Test
    void usesTheCircuitNamedByMain() throws CircuitParseException {
        String xml = """
                <?xml version="1.0"?>
                <project>
                  <lib desc="#Wiring" name="0"/>
                  <main name="top"/>
                  <circuit name="helper"><comp lib="0" loc="(10,10)" name="Pin"><a name="label" val="H"/></comp></circuit>
                  <circuit name="top"><comp lib="0" loc="(10,10)" name="Pin"><a name="label" val="T"/></comp></circuit>
                </project>
                """;
        Circuit circuit = parseXml(xml);
        assertEquals("top", circuit.name());
        assertEquals("T", circuit.inputPins().get(0).label());
    }

    @Test
    void fallsBackToFirstCircuitWithoutMain() throws CircuitParseException {
        String xml = "<?xml version=\"1.0\"?><project><circuit name=\"only\"/></project>";
        assertEquals("only", parseXml(xml).name());
    }

    @Test
    void treatsGatesWithNonStandardOutputAsUnsupported() throws CircuitParseException {
        Circuit circuit = parseXml(circuit(
                "<comp lib=\"1\" loc=\"(300,100)\" name=\"AND Gate\"><a name=\"out\" val=\"0Z\"/></comp>"));
        assertEquals(List.of("W101"), codes(circuit));
        assertTrue(circuit.warnings().get(0).message().contains("default output value setting"));
    }

    @Test
    void readsXorBehaviourForMoreThanTwoInputs() throws CircuitParseException {
        Circuit circuit = parseXml(circuit("""
                <comp lib="1" loc="(300,100)" name="XOR Gate"><a name="inputs" val="3"/></comp>
                <comp lib="1" loc="(300,300)" name="XOR Gate"><a name="inputs" val="3"/><a name="xor" val="odd"/></comp>
                <comp lib="1" loc="(300,500)" name="XNOR Gate"><a name="inputs" val="3"/></comp>
                <comp lib="1" loc="(300,700)" name="XOR Gate"/>
                """));
        assertEquals(List.of(GateType.XOR_ONE, GateType.XOR, GateType.XNOR_ONE, GateType.XOR),
                circuit.gates().stream().map(Gate::type).toList());
    }

    @Test
    void readsConstantsPowerAndGround() throws CircuitParseException {
        Circuit circuit = parseXml(circuit("""
                <comp lib="0" loc="(100,100)" name="Constant"/>
                <comp lib="0" loc="(100,200)" name="Constant"><a name="value" val="0x0"/></comp>
                <comp lib="0" loc="(100,300)" name="Power"/>
                <comp lib="0" loc="(100,400)" name="Ground"/>
                """));
        assertEquals(List.of(true, false, true, false), circuit.components().stream()
                .map(c -> ((entity.circuit.Constant) c).value()).toList());
    }

    @Test
    void warnsAboutVeryLargeCircuits() throws CircuitParseException {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i <= ConnectivityChecker.LARGE_CIRCUIT_COMPONENTS; i++) {
            body.append("<comp lib=\"0\" loc=\"(%d,0)\" name=\"Constant\"/>".formatted(i * 10));
        }
        assertEquals(List.of("W105"), codes(parseXml(circuit(body.toString()))));
    }

    /** Input port positions measured from Logisim-evolution 3.7.2, relative to an east-facing gate. */
    @ParameterizedTest(name = "{0} size {1}")
    @CsvSource({
            "AND Gate, 30, -30, 10", "AND Gate, , -50, 20", "AND Gate, 70, -70, 20",
            "OR Gate, 30, -30, 10", "OR Gate, , -50, 20", "OR Gate, 70, -70, 20",
            "NAND Gate, 30, -40, 10", "NAND Gate, , -60, 20", "NAND Gate, 70, -80, 20",
            "NOR Gate, 30, -40, 10", "NOR Gate, , -60, 20", "NOR Gate, 70, -80, 20",
            "XOR Gate, 30, -40, 10", "XOR Gate, , -60, 20", "XOR Gate, 70, -80, 20",
            "XNOR Gate, 30, -50, 10", "XNOR Gate, , -70, 20", "XNOR Gate, 70, -90, 20",
    })
    void gateInputsSitWhereLogisimPutsThem(String gate, Integer size, int dx, int dy)
            throws CircuitParseException {
        String sizeAttribute = size == null ? "" : "<a name=\"size\" val=\"" + size + "\"/>";
        Circuit circuit = parseXml(circuit(
                "<comp lib=\"1\" loc=\"(500,500)\" name=\"" + gate + "\">" + sizeAttribute + "</comp>"
                        + inputPin(500 + dx, 500 - dy, "A") + inputPin(500 + dx, 500 + dy, "B")));
        assertGateReads(circuit, "A", "B");
    }

    @ParameterizedTest(name = "{0} size {1}")
    @CsvSource({"NOT Gate, , -30", "NOT Gate, 20, -20", "NOT Gate, 30, -30", "Buffer, , -20"})
    void unaryGateInputSitsWhereLogisimPutsIt(String gate, Integer size, int dx) throws CircuitParseException {
        String sizeAttribute = size == null ? "" : "<a name=\"size\" val=\"" + size + "\"/>";
        Circuit circuit = parseXml(circuit(
                "<comp lib=\"1\" loc=\"(500,500)\" name=\"" + gate + "\">" + sizeAttribute + "</comp>"
                        + inputPin(500 + dx, 500, "A")));
        assertGateReads(circuit, "A");
    }

    @Test
    void negatedInputSitsOneBubbleFurtherOut() throws CircuitParseException {
        Circuit circuit = parseXml(circuit(
                "<comp lib=\"1\" loc=\"(500,500)\" name=\"AND Gate\"><a name=\"negate0\" val=\"true\"/></comp>"
                        + inputPin(440, 480, "A") + inputPin(450, 520, "B")));
        assertGateReads(circuit, "A", "B");
        assertEquals(List.of(true, false), circuit.gates().get(0).inputs().stream().map(GateInput::negated).toList());
    }

    private static void assertGateReads(Circuit circuit, String... labels) {
        Map<String, Pin> pins = pinsByLabel(circuit);
        List<Integer> expected = java.util.Arrays.stream(labels).map(l -> pins.get(l).net()).toList();
        assertEquals(expected, circuit.gates().get(0).inputNets());
        assertEquals(List.of(), circuit.warnings());
    }

    @Test
    void loadsFromPath(@TempDir Path dir) throws IOException, CircuitParseException {
        Path file = dir.resolve("half_adder.circ");
        try (InputStream in = getClass().getResourceAsStream("/circuits/half_adder.circ")) {
            Files.copy(in, file);
        }
        assertEquals(2, parser.load(file.toString()).gates().size());
    }

    @Test
    void reportsUnreadableFile(@TempDir Path dir) {
        CircuitParseException e = assertThrows(CircuitParseException.class,
                () -> parser.load(dir.resolve("missing.circ").toString()));
        assertEquals("E001", e.code());
        assertTrue(e.getMessage().startsWith("Could not read the file"));
    }

    @Test
    void rejectsFilesOverTheSizeLimit(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("huge.circ");
        Files.write(file, new byte[(int) LogisimCircuitParser.MAX_FILE_BYTES + 1]);
        CircuitParseException e = assertThrows(CircuitParseException.class, () -> parser.load(file.toString()));
        assertEquals("E001", e.code());
        assertTrue(e.getMessage().contains("larger than 5 MB"));
    }
}
