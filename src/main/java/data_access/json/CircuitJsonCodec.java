package data_access.json;

import entity.circuit.GateType;
import entity.design.CircuitDesign;
import entity.design.GateSpec;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Converts {@link CircuitDesign}s to and from the shared circuit JSON format described in
 * {@code docs/circuit-json.md}.
 *
 * <p>Writing is deterministic (fixed key order, no extra whitespace) so prompts can be tested
 * with golden strings. Reading is lenient about what surrounds the JSON, because AI replies
 * often wrap it in prose or code fences: only the text from the first opening brace to the last
 * closing brace is read. Extra keys are ignored. Everything else is checked, with messages that
 * say what to fix.
 */
public final class CircuitJsonCodec {

    private static final String NAME = "name";
    private static final String INPUTS = "inputs";
    private static final String OUTPUTS = "outputs";
    private static final String GATES = "gates";
    private static final String CONNECTIONS = "connections";
    private static final String ID = "id";
    private static final String TYPE = "type";

    /** @return the design as compact JSON with keys in the documented order */
    public String toJson(CircuitDesign design) {
        JSONStringer json = new JSONStringer();
        json.object();
        json.key(NAME).value(design.name());
        json.key(INPUTS);
        writeStrings(json, design.inputs());
        json.key(OUTPUTS);
        writeStrings(json, design.outputs());
        json.key(GATES).array();
        for (GateSpec gate : design.gates()) {
            json.object();
            json.key(ID).value(gate.id());
            json.key(TYPE).value(gate.type().name());
            json.key(INPUTS);
            writeStrings(json, gate.inputs());
            json.endObject();
        }
        json.endArray();
        json.key(CONNECTIONS).object();
        for (Map.Entry<String, String> connection : design.connections().entrySet()) {
            json.key(connection.getKey()).value(connection.getValue());
        }
        json.endObject();
        json.endObject();
        return json.toString();
    }

    /**
     * @param text JSON, possibly surrounded by other text such as an AI's explanation
     * @throws DesignFormatException if there is no JSON object, it is malformed, a key is missing
     *                               or has the wrong type, a gate type is unknown, or the design
     *                               breaks a rule of {@link CircuitDesign}
     */
    public CircuitDesign fromJson(String text) throws DesignFormatException {
        JSONObject root = parseObject(extractObject(text));
        String name = requireString(root, NAME, "the circuit");
        List<String> inputs = requireStrings(root, INPUTS, "the circuit");
        List<String> outputs = requireStrings(root, OUTPUTS, "the circuit");
        List<GateSpec> gates = readGates(root);
        Map<String, String> connections = readConnections(root);
        try {
            return new CircuitDesign(name, inputs, outputs, gates, connections);
        } catch (IllegalArgumentException e) {
            throw new DesignFormatException(capitalize(e.getMessage()) + ".");
        }
    }

    private static void writeStrings(JSONStringer json, List<String> values) {
        json.array();
        for (String value : values) {
            json.value(value);
        }
        json.endArray();
    }

    private static String extractObject(String text) throws DesignFormatException {
        if (text == null) {
            throw new DesignFormatException("No circuit JSON was given.");
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new DesignFormatException("No JSON object found: expected text between { and }.");
        }
        return text.substring(start, end + 1);
    }

    private static JSONObject parseObject(String json) throws DesignFormatException {
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            throw new DesignFormatException("The circuit JSON is malformed: " + e.getMessage(), e);
        }
    }

    private static List<GateSpec> readGates(JSONObject root) throws DesignFormatException {
        JSONArray array = requireArray(root, GATES, "the circuit");
        List<GateSpec> gates = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            String where = "gate " + (i + 1);
            JSONObject gate = array.optJSONObject(i);
            if (gate == null) {
                throw new DesignFormatException(capitalize(where) + " must be an object with id, type and inputs.");
            }
            String id = requireString(gate, ID, where);
            where = "gate " + id;
            GateType type = gateType(requireString(gate, TYPE, where), where);
            List<String> inputs = requireStrings(gate, INPUTS, where);
            try {
                gates.add(new GateSpec(id, type, inputs));
            } catch (IllegalArgumentException e) {
                throw new DesignFormatException(capitalize(e.getMessage()) + ".");
            }
        }
        return gates;
    }

    private static Map<String, String> readConnections(JSONObject root) throws DesignFormatException {
        JSONObject object = root.optJSONObject(CONNECTIONS);
        if (object == null) {
            throw missingOrWrongType(root, CONNECTIONS, "the circuit", "an object mapping each output to its driver");
        }
        Map<String, String> connections = new LinkedHashMap<>();
        for (String output : object.keySet()) {
            Object driver = object.get(output);
            if (!(driver instanceof String text)) {
                throw new DesignFormatException("Connection " + output + " must be a string naming an input or gate.");
            }
            connections.put(output, text);
        }
        return connections;
    }

    private static GateType gateType(String type, String where) throws DesignFormatException {
        try {
            return GateType.valueOf(type.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new DesignFormatException(capitalize(where) + " has unknown type \"" + type + "\". Use one of: "
                    + String.join(", ", Arrays.stream(GateType.values()).map(Enum::name).toList()) + ".");
        }
    }

    private static String requireString(JSONObject object, String key, String where) throws DesignFormatException {
        Object value = object.opt(key);
        if (!(value instanceof String text)) {
            throw missingOrWrongType(object, key, where, "a string");
        }
        return text;
    }

    private static JSONArray requireArray(JSONObject object, String key, String where) throws DesignFormatException {
        JSONArray array = object.optJSONArray(key);
        if (array == null) {
            throw missingOrWrongType(object, key, where, "a list");
        }
        return array;
    }

    private static List<String> requireStrings(JSONObject object, String key, String where)
            throws DesignFormatException {
        JSONArray array = requireArray(object, key, where);
        List<String> values = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            if (!(array.get(i) instanceof String text)) {
                throw new DesignFormatException(capitalize(where) + ": every entry of \"" + key
                        + "\" must be a string, but entry " + (i + 1) + " is " + array.get(i) + ".");
            }
            values.add(text);
        }
        return values;
    }

    private static DesignFormatException missingOrWrongType(JSONObject object, String key, String where,
                                                            String expected) {
        if (!object.has(key)) {
            return new DesignFormatException(capitalize(where) + " is missing \"" + key + "\".");
        }
        return new DesignFormatException(capitalize(where) + ": \"" + key + "\" must be " + expected + ".");
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
