package view;

import interface_adapter.import_circuit.DrawableComponent;
import interface_adapter.import_circuit.DrawableWire;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.List;
import java.util.Map;

/**
 * Draws an opened circuit: wires as green lines, gates as labelled boxes, input pins as
 * squares, output pins as circles. The drawing is scaled to fit the panel.
 */
public final class CircuitCanvas extends JComponent {

    private static final int PADDING = 30;
    private static final double MAX_SCALE = 2.0;
    private static final Color WIRE = new Color(0, 140, 0);
    private static final Color GATE_FILL = new Color(255, 250, 220);
    private static final Color PIN_FILL = new Color(220, 235, 255);

    /** How far a gate's body reaches back from its output point, as Logisim draws it. */
    private static final Map<String, Integer> GATE_DEPTH = Map.of(
            "NOT", 30, "BUFFER", 20, "NAND", 60, "NOR", 60, "XOR", 60, "XOR_ONE", 60,
            "XNOR", 70, "XNOR_ONE", 70);
    private static final int DEFAULT_GATE_DEPTH = 50;

    private List<DrawableComponent> components = List.of();
    private List<DrawableWire> wires = List.of();

    /** Replaces what is drawn. */
    public void show(List<DrawableComponent> newComponents, List<DrawableWire> newWires) {
        this.components = List.copyOf(newComponents);
        this.wires = List.copyOf(newWires);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (components.isEmpty() && wires.isEmpty()) {
            g.setColor(Color.GRAY);
            drawCentered(g, "Open a .circ file to see your circuit here.", getWidth() / 2, getHeight() / 2);
        } else {
            fitToPanel(g, drawingBounds());
            wires.forEach(wire -> drawWire(g, wire));
            components.forEach(component -> drawComponent(g, component));
        }
        g.dispose();
    }

    private void fitToPanel(Graphics2D g, Rectangle bounds) {
        double availableWidth = getWidth() - 2.0 * PADDING;
        double availableHeight = getHeight() - 2.0 * PADDING;
        double scale = Math.min(MAX_SCALE, Math.min(availableWidth / bounds.width, availableHeight / bounds.height));
        g.translate(PADDING + (availableWidth - bounds.width * scale) / 2,
                PADDING + (availableHeight - bounds.height * scale) / 2);
        g.scale(scale, scale);
        g.translate(-bounds.x, -bounds.y);
    }

    private Rectangle drawingBounds() {
        Rectangle bounds = null;
        for (DrawableWire wire : wires) {
            bounds = union(bounds, new Rectangle(wire.x1(), wire.y1(), 0, 0));
            bounds = union(bounds, new Rectangle(wire.x2(), wire.y2(), 0, 0));
        }
        for (DrawableComponent component : components) {
            bounds = union(bounds, body(component));
        }
        bounds.grow(PADDING, PADDING);
        return bounds;
    }

    private static Rectangle union(Rectangle bounds, Rectangle more) {
        return bounds == null ? more : bounds.union(more);
    }

    private static void drawWire(Graphics2D g, DrawableWire wire) {
        g.setColor(WIRE);
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(wire.x1(), wire.y1(), wire.x2(), wire.y2());
    }

    private static void drawComponent(Graphics2D g, DrawableComponent component) {
        Rectangle box = body(component);
        g.setStroke(new BasicStroke(1.5f));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        switch (component.kind()) {
            case "gate" -> drawBox(g, box, GATE_FILL, gateName(component.detail()));
            case "input" -> drawBox(g, box, PIN_FILL, "");
            case "output" -> {
                g.setColor(PIN_FILL);
                g.fillOval(box.x, box.y, box.width, box.height);
                g.setColor(Color.BLACK);
                g.drawOval(box.x, box.y, box.width, box.height);
            }
            case "constant" -> drawBox(g, box, Color.WHITE, component.detail());
            default -> {
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10,
                        new float[] {4, 4}, 0));
                drawBox(g, box, new Color(235, 235, 235), component.detail());
            }
        }
        if (!component.label().isEmpty()) {
            g.setColor(Color.BLUE.darker());
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            drawCentered(g, component.label(), box.x + box.width / 2, box.y - 8);
        }
    }

    private static void drawBox(Graphics2D g, Rectangle box, Color fill, String text) {
        g.setColor(fill);
        g.fillRoundRect(box.x, box.y, box.width, box.height, 8, 8);
        g.setColor(Color.BLACK);
        g.drawRoundRect(box.x, box.y, box.width, box.height, 8, 8);
        drawCentered(g, text, box.x + box.width / 2, box.y + box.height / 2);
    }

    private static void drawCentered(Graphics2D g, String text, int centerX, int centerY) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, centerX - metrics.stringWidth(text) / 2,
                centerY + (metrics.getAscent() - metrics.getDescent()) / 2);
    }

    private static String gateName(String gateType) {
        return gateType.replace("_ONE", "").replace("BUFFER", "BUF");
    }

    /** The component's outline: it sits behind its connection point, away from where it faces. */
    private static Rectangle body(DrawableComponent component) {
        return switch (component.kind()) {
            case "gate" -> behind(component, GATE_DEPTH.getOrDefault(component.detail(), DEFAULT_GATE_DEPTH), 40);
            case "input", "output" -> behind(component, 20, 20);
            case "constant" -> behind(component, 16, 16);
            default -> new Rectangle(component.x() - 30, component.y() - 15, 60, 30);
        };
    }

    private static Rectangle behind(DrawableComponent component, int depth, int breadth) {
        int x = component.x();
        int y = component.y();
        return switch (component.facing()) {
            case "west" -> new Rectangle(x, y - breadth / 2, depth, breadth);
            case "north" -> new Rectangle(x - breadth / 2, y, breadth, depth);
            case "south" -> new Rectangle(x - breadth / 2, y - depth, breadth, depth);
            default -> new Rectangle(x - depth, y - breadth / 2, depth, breadth);
        };
    }
}
