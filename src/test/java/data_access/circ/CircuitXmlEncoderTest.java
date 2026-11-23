package data_access.circ;

import entity.circuit.Circuit;
import entity.circuit.GateType;
import entity.design.CircuitDesign;
import entity.design.GateSpec;
import entity.simulation.CircuitSimulator;
import entity.truthtable.TruthTableGenerator;
import org.junit.jupiter.api.Test;
import use_case.build_circuit.DesignAssembler;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircuitXmlEncoderTest {
    private final CircuitXmlEncoder encoder = new CircuitXmlEncoder();
    private final TruthTableGenerator tables = new TruthTableGenerator(new CircuitSimulator());

    @Test
    void roundTripsAHalfAdderWithoutWarnings() throws Exception {
        CircuitDesign design = new CircuitDesign("Half adder", List.of("A", "B"),
                List.of("Sum", "Cout"),
                List.of(
                        new GateSpec("G1", GateType.XOR, List.of("A", "B")),
                        new GateSpec("G2", GateType.AND, List.of("A", "B"))),
                Map.of("Sum", "G1", "Cout", "G2"));

        assertRoundTrip(design);
    }

    @Test
    void roundTripsANegatedInputAndAConstant() throws Exception {
        CircuitDesign design = new CircuitDesign("Inverter", List.of("A"), List.of("Y"),
                List.of(new GateSpec("G1", GateType.AND, List.of("!A", "1"))),
                Map.of("Y", "G1"));

        assertRoundTrip(design);
    }

    @Test
    void preservesParityAndExactlyOneModesForThreeInputs() throws Exception {
        CircuitDesign design = new CircuitDesign("XOR modes", List.of("A", "B", "C"),
                List.of("Parity", "ExactlyOne"),
                List.of(
                        new GateSpec("G1", GateType.XOR, List.of("A", "B", "C")),
                        new GateSpec("G2", GateType.XOR_ONE, List.of("A", "B", "C"))),
                Map.of("Parity", "G1", "ExactlyOne", "G2"));

        assertRoundTrip(design);
    }

    @Test
    void escapesSpecialCharactersInTheCircuitName() throws Exception {
        CircuitDesign design = new CircuitDesign("A & B \"example\"", List.of("A"),
                List.of("Y"), List.of(), Map.of("Y", "A"));

        assertRoundTrip(design);
    }

    @Test
    void includesThePokeToolInTheExportedToolbar() throws Exception {
        CircuitDesign design = new CircuitDesign("Direct", List.of("A"), List.of("Y"),
                List.of(), Map.of("Y", "A"));
        Circuit circuit = new DesignAssembler().assemble(design);

        String xml = encoder.encode(circuit);

        var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        var builder = factory.newDocumentBuilder();
        org.w3c.dom.Document document;
        try (var input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            document = builder.parse(input);
        }

        var libraries = document.getElementsByTagName("lib");
        String baseLibrary = "";
        for (int index = 0; index < libraries.getLength(); index++) {
            var library = (org.w3c.dom.Element) libraries.item(index);
            if ("#Base".equals(library.getAttribute("desc"))) {
                baseLibrary = library.getAttribute("name");
            }
        }

        assertTrue(!baseLibrary.isEmpty());

        var toolbar = (org.w3c.dom.Element) document.getElementsByTagName("toolbar").item(0);
        var tools = toolbar.getElementsByTagName("tool");
        boolean hasPokeTool = false;
        for (int index = 0; index < tools.getLength(); index++) {
            var tool = (org.w3c.dom.Element) tools.item(index);
            if ("Poke Tool".equals(tool.getAttribute("name"))
                    && baseLibrary.equals(tool.getAttribute("lib"))) {
                hasPokeTool = true;
            }
        }

        assertTrue(hasPokeTool);
    }

    private void assertRoundTrip(CircuitDesign design) throws Exception {
        Circuit original = new DesignAssembler().assemble(design);
        Circuit positioned = new CircuitLayout(new GateDepthCalculator()).layout(original);

        GatePortLocator gatePorts = new GatePortLocator();
        CircuitWireRouter router = new CircuitWireRouter(
                new CircuitPortCollector(gatePorts),
                new WirePathFinder(new WireRoutingRules(), new GateObstacleChecker(gatePorts)));
        Circuit routed = router.route(positioned);
        String xml = encoder.encode(routed);

        Circuit restored;
        try (ByteArrayInputStream input = new ByteArrayInputStream(
                xml.getBytes(StandardCharsets.UTF_8))) {
            restored = new LogisimCircuitParser().parse(input);
        }

        assertEquals(original.name(), restored.name());
        assertEquals(tables.generate(original), tables.generate(restored));
        assertTrue(restored.warnings().isEmpty(), () -> "Warnings: " + restored.warnings());
    }
}
