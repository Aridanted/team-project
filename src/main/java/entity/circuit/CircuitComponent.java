package entity.circuit;

import java.util.List;
import java.util.OptionalInt;

/**
 * A component of a combinational circuit. Connectivity is expressed with integer net ids;
 * {@link #location()} and {@link #facing()} are layout hints for drawing only.
 *
 * <p>The set of component kinds is closed on purpose (sealed): the supported Logisim subset is
 * fixed by the project scope. New behaviour over components (simulation, drawing, describing)
 * is added in new classes without touching these.
 */
public sealed interface CircuitComponent permits Pin, Constant, Gate, UnsupportedComponent {

    /** @return an id unique within the circuit */
    String id();

    Point location();

    Orientation facing();

    /** @return the nets this component reads, in port order; empty if it reads none */
    List<Integer> inputNets();

    /** @return the net this component drives, if it drives one */
    OptionalInt drivenNet();
}
