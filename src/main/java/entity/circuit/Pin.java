package entity.circuit;

/** A labelled 1-bit connection between the circuit and the outside world. */
public sealed interface Pin extends CircuitComponent permits InputPin, OutputPin {

    /** @return the pin's label; unique (ignoring case) among all pins of a circuit */
    String label();

    /** @return the net this pin is attached to */
    int net();

    /** @return a copy of this pin with a different label */
    Pin withLabel(String newLabel);
}
