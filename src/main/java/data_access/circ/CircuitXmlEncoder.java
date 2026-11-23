package data_access.circ;

import java.util.List;

import entity.circuit.Circuit;
import entity.circuit.CircuitComponent;
import entity.circuit.Constant;
import entity.circuit.Gate;
import entity.circuit.GateType;
import entity.circuit.Pin;
import entity.circuit.Wire;
import entity.circuit.InputPin;
import entity.circuit.OutputPin;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.Locale;

/** Encodes a positioned and routed circuit as Logisim-evolution XML. */
public final class CircuitXmlEncoder {

    /**
     * Encodes the circuit using the gate dimensions expected by the port locator.
     *
     * @param circuit the positioned, routed circuit with normalized unary inputs
     * @return the complete Logisim XML document
     * @throws XMLStreamException if XML encoding fails
     * @throws IllegalArgumentException if a component cannot be exported
     */
    public String encode(Circuit circuit) throws XMLStreamException {
        StringWriter text = new StringWriter();
        XMLStreamWriter xml = XMLOutputFactory.newFactory().createXMLStreamWriter(text);

        xml.writeStartDocument("UTF-8", "1.0");
        xml.writeStartElement("project");
        xml.writeAttribute("source", "3.7.2");
        xml.writeAttribute("version", "1.0");

        library(xml, "0", "#Wiring");
        library(xml, "1", "#Gates");
        library(xml, "2", "#Base");

        xml.writeEmptyElement("main");
        xml.writeAttribute("name", circuit.name());
        xml.writeEmptyElement("mappings");
        writeToolbar(xml);

        xml.writeStartElement("circuit");
        xml.writeAttribute("name", circuit.name());
        attribute(xml, "circuit", circuit.name());

        for (CircuitComponent component : circuit.components()) {
            writeComponent(xml, component);
        }
        for (Wire wire : circuit.wires()) {
            xml.writeEmptyElement("wire");
            xml.writeAttribute("from", wire.from().toString());
            xml.writeAttribute("to", wire.to().toString());
        }

        xml.writeEndElement();
        xml.writeEndElement();
        xml.writeEndDocument();
        xml.close();
        return text.toString();
    }

    private void library(XMLStreamWriter xml, String id, String description) throws XMLStreamException {
        xml.writeEmptyElement("lib");
        xml.writeAttribute("name", id);
        xml.writeAttribute("desc", description);
    }

    private void writeComponent(XMLStreamWriter xml, CircuitComponent component)
            throws XMLStreamException {
        switch (component) {
            case InputPin pin -> writePin(xml, pin, false);
            case OutputPin pin -> writePin(xml, pin, true);
            case Constant constant -> writeConstant(xml, constant);
            case Gate gate -> writeGate(xml, gate);
            default -> throw new IllegalArgumentException(
                    "Cannot export unsupported component " + component.id());
        }
    }

    private void writePin(XMLStreamWriter xml, Pin pin, boolean output) throws XMLStreamException {
        startComponent(xml, pin, "0", "Pin");
        attribute(xml, "appearance", "classic");
        attribute(xml, "label", pin.label());
        attribute(xml, "output", Boolean.toString(output));
        attribute(xml, "tristate", "false");
        xml.writeEndElement();
    }

    private void writeConstant(XMLStreamWriter xml, Constant constant) throws XMLStreamException {
        startComponent(xml, constant, "0", "Constant");
        attribute(xml, "value", constant.value() ? "0x1" : "0x0");
        xml.writeEndElement();
    }

    private void writeGate(XMLStreamWriter xml, Gate gate) throws XMLStreamException {
        startComponent(xml, gate, "1", gateName(gate.type()));

        if (gate.type().isUnary()) {
            if (gate.inputs().getFirst().negated()) {
                throw new IllegalArgumentException("Normalize unary gate " + gate.id() + " before export.");
            }
            if (gate.type() == GateType.NOT) {
                attribute(xml, "size", Integer.toString(GatePortLocator.NOT_WIDTH));
            }
        } else {
            attribute(xml, "size", Integer.toString(GateGeometry.DEFAULT_SIZE));
            attribute(xml, "inputs", Integer.toString(gate.inputs().size()));
            writeInputNegations(xml, gate);
        }

        writeXorMode(xml, gate.type());
        xml.writeEndElement();
    }

    private void writeInputNegations(XMLStreamWriter xml, Gate gate) throws XMLStreamException {
        for (int index = 0; index < gate.inputs().size(); index++) {
            if (gate.inputs().get(index).negated()) {
                attribute(xml, "negate" + index, "true");
            }
        }
    }

    private void writeXorMode(XMLStreamWriter xml, GateType type) throws XMLStreamException {
        switch (type) {
            case XOR, XNOR -> attribute(xml, "xor", "odd");
            case XOR_ONE, XNOR_ONE -> attribute(xml, "xor", "1");
            default -> {
                // Other gate families have no XOR mode.
            }
        }
    }

    private String gateName(GateType type) {
        return switch (type) {
            case AND -> "AND Gate";
            case OR -> "OR Gate";
            case NAND -> "NAND Gate";
            case NOR -> "NOR Gate";
            case XOR, XOR_ONE -> "XOR Gate";
            case XNOR, XNOR_ONE -> "XNOR Gate";
            case NOT -> "NOT Gate";
            case BUFFER -> "Buffer";
        };
    }

    private void startComponent(XMLStreamWriter xml, CircuitComponent component,
                                String library, String name) throws XMLStreamException {
        xml.writeStartElement("comp");
        xml.writeAttribute("lib", library);
        xml.writeAttribute("name", name);
        xml.writeAttribute("loc", component.location().toString());
        attribute(xml, "facing", facing(component));
        attribute(xml, "width", "1");
    }

    private String facing(CircuitComponent component) {
        return switch (component) {
            case Pin pin -> pin.facing().name().toLowerCase(Locale.ROOT);
            case Gate gate -> gate.facing().name().toLowerCase(Locale.ROOT);
            case Constant constant -> constant.facing().name().toLowerCase(Locale.ROOT);
            default -> throw new IllegalArgumentException(
                    "Cannot export unsupported component " + component.id());
        };
    }

    private void attribute(XMLStreamWriter xml, String name, String value) throws XMLStreamException {
        xml.writeEmptyElement("a");
        xml.writeAttribute("name", name);
        xml.writeAttribute("val", value);
    }

    private void writeToolbar(XMLStreamWriter xml) throws XMLStreamException {
        xml.writeStartElement("toolbar");
        for (String tool : List.of("Poke Tool", "Edit Tool", "Wiring Tool", "Text Tool")) {
            xml.writeEmptyElement("tool");
            xml.writeAttribute("lib", "2");
            xml.writeAttribute("name", tool);
        }
        xml.writeEndElement();
    }
}
