package view;

import interface_adapter.import_circuit.DrawableComponent;
import interface_adapter.import_circuit.DrawableWire;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.List;

/**
 * Draws a circuit: wires as lines, gates as labelled boxes, pins as circles with their labels,
 * constants as small boxes with 0 or 1, unsupported components as dashed boxes. The drawing is
 * scaled and centred to fit the panel. Reused by the Builder tab.
 */
public final class CircuitCanvas extends JComponent {

    private static final long serialVersionUID = 1L;

    static final int MARGIN = 24;
    static final double MAX_SCALE = 2.5;
    private static final int GATE_LENGTH = 50;
    private static final int GATE_WIDTH = 40;
    private static final int UNARY_LENGTH = 30;
    private static final int UNARY_WIDTH = 20;
    private static final int PIN_RADIUS = 7;
    private static final int LABEL_ROOM = 60;

    private static final Color BACKGROUND = Color.WHITE;
    private static final Color WIRE = new Color(25, 118, 210);
    private static final Color OUTLINE = new Color(33, 33, 33);
    private static final Color GATE_FILL = new Color(243, 244, 248);
    private static final Color PIN_FILL = new Color(255, 248, 225);
    private static final Color UNSUPPORTED = new Color(176, 0, 32);
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);

    private transient List<DrawableComponent> components = List.of();
    private transient List<DrawableWire> wires = List.of();
    private String emptyMessage = "Open a .circ file to see the circuit here.";

    public CircuitCanvas() {
        setPreferredSize(new Dimension(600, 380));
        setOpaque(true);
    }

    public void setCircuit(List<DrawableComponent> components, List<DrawableWire> wires) {
        this.components = List.copyOf(components);
        this.wires = List.copyOf(wires);
        repaint();
    }

    public void setEmptyMessage(String message) {
        this.emptyMessage = message;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(BACKGROUND);
            g.fillRect(0, 0, getWidth(), getHeight());
            if (components.isEmpty() && wires.isEmpty()) {
                paintEmpty(g);
                return;
            }
            Rectangle bounds = bounds(components, wires);
            double scale = scale(bounds, getWidth(), getHeight());
            g.translate((getWidth() - bounds.width * scale) / 2, (getHeight() - bounds.height * scale) / 2);
            g.scale(scale, scale);
            g.translate(-bounds.x, -bounds.y);
            paintWires(g);
            for (DrawableComponent component : components) {
                paintComponentShape(g, component);
            }
        } finally {
            g.dispose();
        }
    }

    /** @return the area the drawing covers, in circuit coordinates, including labels */
    static Rectangle bounds(List<DrawableComponent> components, List<DrawableWire> wires) {
        Rectangle bounds = null;
        for (DrawableWire wire : wires) {
            bounds = union(bounds, new Rectangle(Math.min(wire.x1(), wire.x2()), Math.min(wire.y1(), wire.y2()),
                    Math.abs(wire.x2() - wire.x1()), Math.abs(wire.y2() - wire.y1())));
        }
        for (DrawableComponent component : components) {
            bounds = union(bounds, footprint(component));
        }
        return bounds == null ? new Rectangle(0, 0, 1, 1) : bounds;
    }

    /** @return how much to enlarge the drawing so it fits {@code width x height} with a margin */
    static double scale(Rectangle bounds, int width, int height) {
        double available = Math.max(1, width - 2 * MARGIN);
        double availableHeight = Math.max(1, height - 2 * MARGIN);
        double fit = Math.min(available / Math.max(1, bounds.width), availableHeight / Math.max(1, bounds.height));
        return Math.min(MAX_SCALE, fit);
    }

    private static Rectangle union(Rectangle bounds, Rectangle next) {
        return bounds == null ? next : bounds.union(next);
    }

    private static Rectangle footprint(DrawableComponent component) {
        int x = component.x();
        int y = component.y();
        return switch (component.kind()) {
            case "gate", "unsupported" -> body(component);
            case "input" -> new Rectangle(x - LABEL_ROOM, y - PIN_RADIUS * 2, LABEL_ROOM + PIN_RADIUS, PIN_RADIUS * 4);
            case "output" -> new Rectangle(x - PIN_RADIUS, y - PIN_RADIUS * 2, LABEL_ROOM + PIN_RADIUS, PIN_RADIUS * 4);
            default -> new Rectangle(x - PIN_RADIUS * 2, y - PIN_RADIUS, PIN_RADIUS * 2, PIN_RADIUS * 2);
        };
    }

    /** The body of a gate sits behind its output point, which is the component's location. */
    private static Rectangle body(DrawableComponent component) {
        boolean unary = "NOT".equals(component.detail()) || "BUFFER".equals(component.detail());
        int length = unary ? UNARY_LENGTH : GATE_LENGTH;
        int width = unary ? UNARY_WIDTH : GATE_WIDTH;
        int x = component.x();
        int y = component.y();
        return switch (component.facing()) {
            case "west" -> new Rectangle(x, y - width / 2, length, width);
            case "north" -> new Rectangle(x - width / 2, y, width, length);
            case "south" -> new Rectangle(x - width / 2, y - length, width, length);
            default -> new Rectangle(x - length, y - width / 2, length, width);
        };
    }

    private void paintEmpty(Graphics2D g) {
        g.setColor(Styles.MUTED);
        FontMetrics metrics = g.getFontMetrics();
        int x = (getWidth() - metrics.stringWidth(emptyMessage)) / 2;
        g.drawString(emptyMessage, Math.max(MARGIN, x), getHeight() / 2);
    }

    private void paintWires(Graphics2D g) {
        g.setColor(WIRE);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (DrawableWire wire : wires) {
            g.drawLine(wire.x1(), wire.y1(), wire.x2(), wire.y2());
        }
    }

    private void paintComponentShape(Graphics2D g, DrawableComponent component) {
        g.setStroke(new BasicStroke(1.5f));
        g.setFont(LABEL_FONT);
        switch (component.kind()) {
            case "gate" -> paintBox(g, body(component), gateName(component.detail()), GATE_FILL, OUTLINE, null);
            case "unsupported" -> paintBox(g, body(component), component.detail(), Color.WHITE, UNSUPPORTED,
                    new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {4f, 3f}, 0f));
            case "input" -> paintPin(g, component, true);
            case "output" -> paintPin(g, component, false);
            case "constant" -> paintBox(g, footprint(component), component.detail(), GATE_FILL, OUTLINE, null);
            default -> {
                // Unknown kinds are not drawn.
            }
        }
    }

    private static void paintBox(Graphics2D g, Rectangle box, String text, Color fill, Color outline, Stroke stroke) {
        g.setColor(fill);
        g.fillRoundRect(box.x, box.y, box.width, box.height, 8, 8);
        g.setColor(outline);
        Stroke previous = g.getStroke();
        if (stroke != null) {
            g.setStroke(stroke);
        }
        g.drawRoundRect(box.x, box.y, box.width, box.height, 8, 8);
        g.setStroke(previous);
        drawCentred(g, text, box.x + box.width / 2, box.y + box.height / 2);
    }

    private static void paintPin(Graphics2D g, DrawableComponent pin, boolean input) {
        int x = pin.x();
        int y = pin.y();
        g.setColor(PIN_FILL);
        g.fillOval(x - PIN_RADIUS, y - PIN_RADIUS, PIN_RADIUS * 2, PIN_RADIUS * 2);
        g.setColor(OUTLINE);
        g.drawOval(x - PIN_RADIUS, y - PIN_RADIUS, PIN_RADIUS * 2, PIN_RADIUS * 2);
        FontMetrics metrics = g.getFontMetrics();
        int baseline = y + metrics.getAscent() / 2 - 1;
        if (input) {
            g.drawString(pin.label(), x - PIN_RADIUS - 4 - metrics.stringWidth(pin.label()), baseline);
        } else {
            g.drawString(pin.label(), x + PIN_RADIUS + 4, baseline);
        }
    }

    private static void drawCentred(Graphics2D g, String text, int centreX, int centreY) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, centreX - metrics.stringWidth(text) / 2, centreY + metrics.getAscent() / 2 - 1);
    }

    /** @return a short name for a gate type, for example "XOR1" for XOR_ONE */
    static String gateName(String type) {
        return type.replace("_ONE", "1");
    }
}
