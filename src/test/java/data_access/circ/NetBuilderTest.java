package data_access.circ;

import entity.circuit.Point;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetBuilderTest {

    private final NetBuilder builder = new NetBuilder();

    private static Point p(int x, int y) {
        return new Point(x, y);
    }

    private static RawCircuit.RawWire w(int x1, int y1, int x2, int y2) {
        return new RawCircuit.RawWire(p(x1, y1), p(x2, y2));
    }

    @Test
    void wireConnectsItsEndpoints() {
        NetMap nets = builder.build(List.of(w(0, 0, 50, 0)), List.of());
        assertEquals(nets.netAt(p(0, 0)), nets.netAt(p(50, 0)));
        assertEquals(1, nets.netCount());
    }

    @Test
    void endpointOnAnotherWiresInteriorConnects() {
        NetMap nets = builder.build(List.of(w(0, 0, 100, 0), w(50, 0, 50, 80)), List.of());
        assertEquals(nets.netAt(p(0, 0)), nets.netAt(p(50, 80)));
    }

    @Test
    void portOnWireInteriorConnects() {
        NetMap nets = builder.build(List.of(w(0, 0, 100, 0)), List.of(p(40, 0)));
        assertEquals(nets.netAt(p(0, 0)), nets.netAt(p(40, 0)));
    }

    @Test
    void crossingWithoutSharedPointDoesNotConnect() {
        NetMap nets = builder.build(List.of(w(0, 50, 100, 50), w(50, 0, 50, 100)), List.of());
        assertNotEquals(nets.netAt(p(0, 50)), nets.netAt(p(50, 0)));
        assertEquals(2, nets.netCount());
    }

    @Test
    void unattachedPortGetsItsOwnNetWithoutWire() {
        NetMap nets = builder.build(List.of(w(0, 0, 10, 0)), List.of(p(100, 100)));
        int net = nets.netAt(p(100, 100));
        assertNotEquals(nets.netAt(p(0, 0)), net);
        assertFalse(nets.hasWire(net));
        assertTrue(nets.hasWire(nets.netAt(p(0, 0))));
    }

    @Test
    void reportsOnlyTrulyDanglingWireEnds() {
        NetMap nets = builder.build(
                List.of(w(0, 0, 100, 0), w(50, 0, 50, 50), w(100, 0, 100, 30)),
                List.of(p(0, 0)));
        // (0,0) is a port, (50,0) is a T-junction, (100,0) is shared: only (50,50) and (100,30) dangle
        assertEquals(List.of(p(50, 50), p(100, 30)), nets.danglingWireEnds());
    }

    @Test
    void numbersNetsDeterministicallyBySmallestPoint() {
        NetMap nets = builder.build(List.of(w(200, 0, 300, 0), w(0, 0, 0, 100)), List.of(p(100, 50)));
        assertEquals(0, nets.netAt(p(0, 0)));
        assertEquals(1, nets.netAt(p(100, 50)));
        assertEquals(2, nets.netAt(p(200, 0)));
    }
}
