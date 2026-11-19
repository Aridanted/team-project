package view;

import interface_adapter.check_circuit.ResultRow;
import interface_adapter.import_circuit.DrawableComponent;
import interface_adapter.import_circuit.DrawableWire;
import interface_adapter.tutor_chat.ChatBubble;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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

    @Test
    void resultsTableExportsAsCsvWithQuotedValues() {
        CheckResultsTableModel model = new CheckResultsTableModel();
        assertFalse(model.hasResults());
        model.setResults(List.of("A"), List.of("Y"), List.of(
                new ResultRow(List.of("0"), List.of("1"), List.of("0"), false, "FAIL (Y)")));

        assertTrue(model.hasResults());
        assertEquals("A,Y expected,Y actual,Result\n0,1,0,FAIL (Y)\n", model.toCsv());
        assertEquals("\"a,b\"", CheckResultsTableModel.csvValue("a,b"));
        assertEquals("\"say \"\"hi\"\"\"", CheckResultsTableModel.csvValue("say \"hi\""));
    }

    @Test
    void canvasExportsTheCircuitAtTwiceItsSize() {
        CircuitCanvas canvas = new CircuitCanvas();
        assertTrue(canvas.isEmpty());
        canvas.setCircuit(List.of(new DrawableComponent("gate", "AND", "", 200, 100, "east")),
                List.of(new DrawableWire(100, 80, 150, 80)));

        BufferedImage image = canvas.toImage();

        assertFalse(canvas.isEmpty());
        assertEquals(100 * CircuitCanvas.EXPORT_SCALE + 2 * CircuitCanvas.MARGIN, image.getWidth());
        assertEquals(40 * CircuitCanvas.EXPORT_SCALE + 2 * CircuitCanvas.MARGIN, image.getHeight());
    }

    @Test
    void downloadAddsTheExtensionOnlyWhenMissing(@TempDir Path folder) throws IOException {
        File plain = folder.resolve("work").toFile();
        File named = folder.resolve("work.CSV").toFile();

        assertEquals("work.csv", DownloadBar.withExtension(plain, ".csv").getName());
        assertEquals("work.CSV", DownloadBar.withExtension(named, ".csv").getName());
        assertEquals(".csv", new DownloadBar.Item("Results", "results.csv", Optional::empty, "none").extension());

        Path saved = folder.resolve("chat.txt");
        DownloadBar.text("You: hi\n").writeTo(saved);
        assertEquals("You: hi\n", Files.readString(saved));
    }

    @Test
    void transcriptTextNamesEachSender() {
        assertEquals("You: Why?\n\nTutor: Look at row 3.\n\n", TutorChatView.transcriptText(List.of(
                new ChatBubble(true, "You", "Why?"), new ChatBubble(false, "Tutor", "Look at row 3."))));
    }

    @Test
    void bubblesFitShortTextAndWrapLongText() {
        ChatBubbleList list = new ChatBubbleList();
        list.setSize(500, 400);
        String longText = "Look at what the OR gate gets when both B and C are 1. ".repeat(6);
        list.setMessages(List.of(new ChatBubble(true, "You", "Hi"), new ChatBubble(false, "Tutor", longText)),
                "intro");

        List<ChatBubbleList.Bubble> bubbles = list.bubbles();
        int limit = list.maxBubbleWidth();
        Dimension shortBubble = bubbles.get(0).getPreferredSize();
        Dimension longBubble = bubbles.get(1).getPreferredSize();

        assertEquals(2, bubbles.size());
        assertTrue(shortBubble.width < limit / 2, "a short message gets a narrow bubble");
        assertEquals(limit, longBubble.width, "a long message is capped at the limit");
        assertTrue(longBubble.height > shortBubble.height * 2, "and wraps onto several lines");
    }

    @Test
    void emptyConversationShowsTheIntro() {
        ChatBubbleList list = new ChatBubbleList();
        list.setMessages(List.of(), "Ask the tutor.");

        assertEquals("Ask the tutor.", list.bubbles().get(0).getText());
    }
}
