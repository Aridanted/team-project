package data_access.circ;

import entity.circuit.Orientation;
import entity.circuit.Point;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Expected values were read from Logisim-evolution 3.7.2's own component factories. */
class GateGeometryTest {

    @ParameterizedTest(name = "size {0}, {1} inputs")
    @CsvSource({
            "30, 2, -10 10",
            "30, 3, -10 0 10",
            "30, 4, -20 -10 10 20",
            "30, 5, -20 -10 0 10 20",
            "30, 6, -30 -20 -10 10 20 30",
            "30, 7, -30 -20 -10 0 10 20 30",
            "30, 8, -40 -30 -20 -10 10 20 30 40",
            "30, 9, -40 -30 -20 -10 0 10 20 30 40",
            "50, 2, -20 20",
            "50, 3, -20 0 20",
            "50, 4, -20 -10 10 20",
            "50, 5, -20 -10 0 10 20",
            "50, 6, -30 -20 -10 10 20 30",
            "50, 7, -30 -20 -10 0 10 20 30",
            "50, 8, -40 -30 -20 -10 10 20 30 40",
            "50, 9, -40 -30 -20 -10 0 10 20 30 40",
            "70, 2, -20 20",
            "70, 3, -30 0 30",
            "70, 4, -30 -10 10 30",
            "70, 5, -20 -10 0 10 20",
            "70, 6, -30 -20 -10 10 20 30",
            "70, 7, -30 -20 -10 0 10 20 30",
            "70, 8, -40 -30 -20 -10 10 20 30 40",
            "70, 9, -40 -30 -20 -10 0 10 20 30 40",
    })
    void inputOffsetsMatchLogisim(int size, int inputs, String expected) {
        List<Integer> actual = new ArrayList<>();
        for (int i = 0; i < inputs; i++) {
            actual.add(GateGeometry.inputOffset(size, inputs, i));
        }
        assertEquals(Arrays.stream(expected.split(" ")).map(Integer::valueOf).toList(), actual);
    }

    @ParameterizedTest(name = "facing {0}")
    @CsvSource({
            "EAST, 450, 480, 450, 520",
            "WEST, 550, 480, 550, 520",
            "NORTH, 480, 550, 520, 550",
            "SOUTH, 480, 450, 520, 450",
    })
    void rotationMatchesLogisimForDefaultAndGate(Orientation facing, int x0, int y0, int x1, int y1) {
        Point origin = new Point(500, 500);
        assertEquals(new Point(x0, y0), GateGeometry.place(origin, -50, -20, facing));
        assertEquals(new Point(x1, y1), GateGeometry.place(origin, -50, 20, facing));
    }

    @Test
    void singleInputSitsOnTheAxis() {
        Point origin = new Point(500, 500);
        assertEquals(new Point(470, 500), GateGeometry.place(origin, -30, 0, Orientation.EAST));
        assertEquals(new Point(500, 530), GateGeometry.place(origin, -30, 0, Orientation.NORTH));
        assertEquals(new Point(500, 470), GateGeometry.place(origin, -30, 0, Orientation.SOUTH));
        assertEquals(new Point(530, 500), GateGeometry.place(origin, -30, 0, Orientation.WEST));
    }
}
