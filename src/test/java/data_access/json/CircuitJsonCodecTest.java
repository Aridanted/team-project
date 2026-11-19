package data_access.json;

import data_access.circ.LogisimCircuitParser;
import entity.circuit.Circuit;
import entity.circuit.GateType;
import entity.design.CircuitDescriber;
import entity.design.CircuitDesign;
import entity.design.GateSpec;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircuitJsonCodecTest {

    private static final String HALF_ADDER_JSON = "{\"name\":\"half adder\",\"inputs\":[\"A\",\"B\"],"
            + "\"outputs\":[\"Sum\",\"Cout\"],\"gates\":[{\"id\":\"g1\",\"type\":\"XOR\",\"inputs\":[\"A\",\"B\"]},"
            + "{\"id\":\"g2\",\"type\":\"AND\",\"inputs\":[\"A\",\"!B\"]}],"
            + "\"connections\":{\"Sum\":\"g1\",\"Cout\":\"g2\"}}";

    private final CircuitJsonCodec codec = new CircuitJsonCodec();

    private static CircuitDesign halfAdder() {
        return new CircuitDesign("half adder", List.of("A", "B"), List.of("Sum", "Cout"),
                List.of(new GateSpec("g1", GateType.XOR, List.of("A", "B")),
                        new GateSpec("g2", GateType.AND, List.of("A", "!B"))),
                Map.of("Sum", "g1", "Cout", "g2"));
    }

    @Test
    void writesKeysInDocumentedOrder() {
        assertEquals(HALF_ADDER_JSON, codec.toJson(halfAdder()));
    }

    @Test
    void roundTrips() throws DesignFormatException {
        assertEquals(halfAdder(), codec.fromJson(codec.toJson(halfAdder())));
    }

    @Test
    void escapesSpecialCharactersInName() throws DesignFormatException {
        CircuitDesign design = new CircuitDesign("say \"hi\" {now}", List.of("A"), List.of("Y"), List.of(),
                Map.of("Y", "A"));

        assertEquals(design, codec.fromJson(codec.toJson(design)));
    }

    @Test
    void readsJsonSurroundedByProseAndCodeFences() throws DesignFormatException {
        String reply = "Here is your circuit:\n```json\n" + HALF_ADDER_JSON + "\n```\nIt adds two bits.";

        assertEquals(halfAdder(), codec.fromJson(reply));
    }

    @Test
    void acceptsLowerCaseGateTypesAndIgnoresExtraKeys() throws DesignFormatException {
        String json = HALF_ADDER_JSON.replace("\"XOR\"", "\"xor\"").replace("{\"name\"",
                "{\"explanation\":\"two bits\",\"name\"");

        assertEquals(halfAdder(), codec.fromJson(json));
    }

    @Test
    void rejectsTextWithoutJson() {
        assertRejected("No JSON object found", "I cannot help with that.");
        assertRejected("No JSON object found", "} backwards {");
        assertRejected("No circuit JSON was given", null);
    }

    @Test
    void rejectsMalformedJson() {
        assertRejected("The circuit JSON is malformed", "{\"name\": \"x\" \"inputs\": [}");
    }

    @Test
    void rejectsMissingKeys() {
        for (String key : List.of("name", "inputs", "outputs", "gates", "connections")) {
            String json = codec.toJson(halfAdder()).replace("\"" + key + "\":", "\"other_" + key + "\":");
            assertRejected("The circuit is missing \"" + key + "\".", json);
        }
        assertRejected("Gate 1 is missing \"id\".", HALF_ADDER_JSON.replace("\"id\":\"g1\",", ""));
        assertRejected("Gate g1 is missing \"type\".", HALF_ADDER_JSON.replace("\"type\":\"XOR\",", ""));
    }

    @Test
    void rejectsWrongTypes() {
        assertRejected("\"name\" must be a string", HALF_ADDER_JSON.replace("\"half adder\"", "42"));
        assertRejected("\"inputs\" must be a list", HALF_ADDER_JSON.replace("[\"A\",\"B\"],\"outputs\"",
                "\"A\",\"outputs\""));
        assertRejected("every entry of \"inputs\" must be a string, but entry 2 is 7",
                HALF_ADDER_JSON.replace("\"inputs\":[\"A\",\"B\"],\"outputs\"", "\"inputs\":[\"A\",7],\"outputs\""));
        assertRejected("Gate 2 must be an object", HALF_ADDER_JSON.replace(
                "{\"id\":\"g2\",\"type\":\"AND\",\"inputs\":[\"A\",\"!B\"]}", "\"g2\""));
        assertRejected("\"connections\" must be an object", HALF_ADDER_JSON.replace(
                "{\"Sum\":\"g1\",\"Cout\":\"g2\"}", "[]"));
        assertRejected("Connection Cout must be a string", HALF_ADDER_JSON.replace("\"Cout\":\"g2\"", "\"Cout\":2"));
    }

    @Test
    void rejectsUnknownGateType() {
        assertRejected("Gate g1 has unknown type \"MAJORITY\". Use one of: AND, OR, NAND, NOR, XOR, XNOR, XOR_ONE,"
                + " XNOR_ONE, NOT, BUFFER.", HALF_ADDER_JSON.replace("XOR", "MAJORITY"));
    }

    @Test
    void rejectsUndeclaredSignalsWithDesignMessages() {
        assertRejected("Gate g2 uses C, which is not a declared input or gate.", HALF_ADDER_JSON.replace("!B", "C"));
        assertRejected("Output Cout is not connected to anything.", HALF_ADDER_JSON.replace(",\"Cout\":\"g2\"", ""));
        assertRejected("Gate g1 is a XOR gate and needs 2 to 32 inputs, not 1.",
                HALF_ADDER_JSON.replace("\"inputs\":[\"A\",\"B\"]}", "\"inputs\":[\"A\"]}"));
    }

    @Test
    void describesAParsedCircuitFile() throws Exception {
        Circuit circuit = new LogisimCircuitParser().load(fixture("half_adder.circ").toString());

        CircuitDesign design = new CircuitDescriber().describe(circuit);

        assertEquals(List.of("A", "B"), design.inputs());
        assertEquals(2, design.gates().size());
        assertEquals(design, codec.fromJson(codec.toJson(design)));
    }

    private void assertRejected(String expectedPart, String text) {
        DesignFormatException e = assertThrows(DesignFormatException.class, () -> codec.fromJson(text));
        assertTrue(e.getMessage().contains(expectedPart), () -> "message was: " + e.getMessage());
    }

    private static Path fixture(String name) throws Exception {
        return Path.of(CircuitJsonCodecTest.class.getResource("/circuits/" + name).toURI());
    }
}
