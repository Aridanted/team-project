package data_access.circ;

import entity.circuit.Orientation;
import entity.circuit.Point;
import use_case.import_circuit.CircuitParseException;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A {@code <comp>} element. Logisim stores only attributes that differ from the component's
 * defaults, so every accessor takes the default to use when the attribute is absent.
 *
 * @param library    the library description, for example "#Gates"; empty for subcircuits
 * @param name       the component name within its library, for example "AND Gate"
 * @param location   the {@code loc} attribute: for gates the output point, for pins the only point
 * @param attributes the {@code <a name val>} children
 */
record RawComponent(String library, String name, Point location, Map<String, String> attributes) {

    RawComponent {
        attributes = Map.copyOf(attributes);
    }

    Optional<String> attribute(String key) {
        return Optional.ofNullable(attributes.get(key));
    }

    String attribute(String key, String fallback) {
        return attributes.getOrDefault(key, fallback);
    }

    boolean booleanAttribute(String key) {
        return "true".equals(attributes.get(key));
    }

    int intAttribute(String key, int fallback) throws CircuitParseException {
        String value = attributes.get(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw Diagnostics.malformed("attribute " + key + "=\"" + value + "\" of " + name + " at " + location);
        }
    }

    /** @return the bit width; 1 unless the {@code width} attribute says otherwise */
    int width() throws CircuitParseException {
        return intAttribute("width", 1);
    }

    Orientation facing(Orientation fallback) throws CircuitParseException {
        String value = attributes.get("facing");
        if (value == null) {
            return fallback;
        }
        try {
            return Orientation.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw Diagnostics.malformed("facing \"" + value + "\" of " + name + " at " + location);
        }
    }
}
