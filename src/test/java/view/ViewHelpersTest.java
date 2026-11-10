package view;

import interface_adapter.check_circuit.ResultRow;
import interface_adapter.import_circuit.DrawableComponent;
import interface_adapter.import_circuit.DrawableWire;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Logic in the views that can be checked without showing a window. */
class ViewHelpersTest {

    @Test
    void resultsTableHasExpectedAndActualColumnsPerOutput() {
        CheckResultsTableModel model = new CheckResultsTableModel();
        model.setResults(List.of("A", "B"), List.of("Sum"), List.of(
                new ResultRow(List.of("0", "1"), List.of("1"), List.of("1"), true, "PASS"),
                new ResultRow(List.of("1", "1"), List.of("0"), List.of("1"), false, "FAIL (Sum)")));

        assertEquals(List.of("A", "B", "Sum expected", "Sum actual", "Result"), model.columnNames());
        assertEquals(2, model.getRowCount());
        assertEquals("1", model.getValueAt(1, 0));
        assertEquals("0", model.getValueAt(1, 2));
        assertEquals("1", model.getValueAt(1, 3));
        assertEquals("FAIL (Sum)", model.getValueAt(1, 4));
        assertTrue(model.passed(0));
        assertFalse(model.passed(1));

        model.clear();
        assertEquals(0, model.getColumnCount());
    }

    @Test
    void canvasBoundsCoverWiresAndGateBodies() {
        Rectangle bounds = CircuitCanvas.bounds(
                List.of(new DrawableComponent("gate", "AND", "", 200, 100, "east")),
                List.of(new DrawableWire(100, 80, 150, 80)));

        assertEquals(100, bounds.x);
        assertEquals(80, bounds.y);
        assertEquals(200, bounds.x + bounds.width);
        assertEquals(120, bounds.y + bounds.height);
    }

    @Test
    void canvasScalesToFitButNeverHuge() {
        assertEquals(CircuitCanvas.MAX_SCALE, CircuitCanvas.scale(new Rectangle(0, 0, 10, 10), 800, 600));
        assertEquals(0.5, CircuitCanvas.scale(new Rectangle(0, 0, 1000, 100), 500 + 2 * CircuitCanvas.MARGIN, 600));
    }

    @Test
    void canvasPaintsEveryKindWithoutErrors() {
        CircuitCanvas canvas = new CircuitCanvas();
        canvas.setSize(400, 300);
        canvas.setCircuit(List.of(
                new DrawableComponent("input", "", "A", 50, 50, "east"),
                new DrawableComponent("gate", "XOR_ONE", "", 150, 50, "north"),
                new DrawableComponent("gate", "NOT", "", 150, 120, "west"),
                new DrawableComponent("constant", "1", "", 50, 120, "south"),
                new DrawableComponent("unsupported", "D Flip-Flop", "", 250, 120, "east"),
                new DrawableComponent("output", "", "Y", 250, 50, "west")),
                List.of(new DrawableWire(50, 50, 100, 50)));
        BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);

        canvas.paint(image.getGraphics());

        assertEquals("XOR1", CircuitCanvas.gateName("XOR_ONE"));
    }
}
