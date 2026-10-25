package data_access.circ;

import entity.circuit.Point;

/** A {@code <wire>} element: a straight segment between two grid points. */
record RawWire(Point from, Point to) {
}
