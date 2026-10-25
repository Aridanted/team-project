package data_access.circ;

import entity.circuit.InputPin;
import entity.circuit.Orientation;
import entity.circuit.OutputPin;
import entity.circuit.Point;
import use_case.import_circuit.CircuitParseException;

import java.util.List;
import java.util.Optional;

/** Logisim's Wiring &gt; Pin: an input pin, or an output pin when {@code output="true"}. */
final class PinDefinition implements ComponentDefinition {

    @Override
    public PlacedComponent place(RawComponent raw) throws CircuitParseException {
        ComponentDefinition.requireSingleBit(raw);
        Point at = raw.location();
        Orientation facing = raw.facing(Orientation.EAST);
        String label = raw.attribute("label", "").trim();
        if (raw.booleanAttribute("output")) {
            return new PlacedComponent(List.of(at), Optional.empty(),
                    (id, nets) -> new OutputPin(id, label, nets.netAt(at), at, facing));
        }
        return new PlacedComponent(List.of(), Optional.of(at),
                (id, nets) -> new InputPin(id, label, nets.netAt(at), at, facing));
    }
}
