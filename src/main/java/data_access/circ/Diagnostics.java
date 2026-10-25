package data_access.circ;

import entity.circuit.CircuitWarning;
import entity.circuit.GateType;
import entity.circuit.Point;
import use_case.import_circuit.CircuitParseException;

/** The catalogue of parse errors (E0xx) and warnings (W1xx), with their user-facing messages. */
final class Diagnostics {

    private Diagnostics() {
    }

    static CircuitParseException notACircuitFile() {
        return new CircuitParseException("E001", "This is not a valid Logisim circuit file.");
    }

    static CircuitParseException malformed(String detail) {
        return new CircuitParseException("E001",
                "This is not a valid Logisim circuit file (unexpected " + detail + ").");
    }

    static CircuitParseException fileTooLarge(long maxBytes) {
        return new CircuitParseException("E001", "This file is larger than "
                + maxBytes / (1024 * 1024) + " MB, so it is not a valid Logisim circuit file.");
    }

    static CircuitParseException unreadableFile(String path) {
        return new CircuitParseException("E001", "Could not read the file " + path + ".");
    }

    static CircuitParseException noCircuit() {
        return new CircuitParseException("E002", "No circuit found in this file.");
    }

    static CircuitParseException duplicateLabel(String label, Point first, Point second) {
        return new CircuitParseException("E003", "Two pins are both labelled \"" + label + "\" (at "
                + first + " and " + second + "). Give each pin a unique label.");
    }

    static CircuitParseException contention(Point near) {
        return new CircuitParseException("E004", "Two outputs are connected together near " + near + ".");
    }

    static CircuitParseException multiBit(String name, Point at, int width) {
        return new CircuitParseException("E005", "Multi-bit components are not supported: " + name
                + " at " + at + " has width " + width + ". Use 1-bit pins and gates.");
    }

    static CircuitWarning unsupported(String name, Point at, String reason) {
        String message = "Unsupported component: " + name + " at " + at + ". Simulation is disabled.";
        return new CircuitWarning("W101", reason.isEmpty() ? message : message + " " + reason);
    }

    static CircuitWarning floatingGateInput(GateType type, int index, Point gateAt) {
        return new CircuitWarning("W102", "Input " + (index + 1) + " of the " + type + " gate at "
                + gateAt + " is not connected (treated as unknown).");
    }

    static CircuitWarning floatingOutputPin(String label, Point at) {
        return new CircuitWarning("W102", "Output pin " + label + " at " + at
                + " is not connected (treated as unknown).");
    }

    static CircuitWarning unlabelledPin(Point at, String assignedLabel) {
        return new CircuitWarning("W103", "A pin at " + at + " has no label; using " + assignedLabel + ".");
    }

    static CircuitWarning geometryMismatch(Point at) {
        return new CircuitWarning("W104", "A wire ends near " + at + " but nothing is attached.");
    }

    static CircuitWarning largeCircuit(int componentCount) {
        return new CircuitWarning("W105", "Large circuit (" + componentCount
                + " components); display may be slow.");
    }
}
