import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Generates the source .circ fixtures into build/. They are then re-saved by Logisim itself
 * (Normalize.java). Run from this folder with: java GenerateFixtures.java
 */
public class GenerateFixtures {

    private static final Path OUT = Path.of("build");
    private static final Map<String, Integer> EXTRA = Map.of("AND Gate", 0, "OR Gate", 0,
            "NAND Gate", 10, "NOR Gate", 10, "XOR Gate", 10, "XNOR Gate", 20);
    private static final Map<Integer, Map<Integer, int[]>> WIDE = Map.of(
            50, Map.of(2, new int[]{-20, 20}, 3, new int[]{-20, 0, 20}),
            70, Map.of(2, new int[]{-20, 20}, 3, new int[]{-30, 0, 30}, 4, new int[]{-30, -10, 10, 30}));
    private static final Map<String, P> OUTWARD = Map.of("east", new P(-1, 0), "west", new P(1, 0),
            "north", new P(0, 1), "south", new P(0, -1));
    private static final Map<String, String> OPPOSITE = Map.of("east", "west", "west", "east",
            "north", "south", "south", "north");

    record P(int x, int y) {
    }

    record Comp(String lib, String name, int x, int y, Map<String, String> attrs) {
    }

    record Wire(P a, P b) {
    }

    record SubCircuit(String name, List<Comp> comps, List<Wire> wires) {
    }

    record Gate(List<P> ports, P out) {
    }

    static int dy(int size, int n, int i) {
        int[] wide = WIDE.getOrDefault(size, Map.of()).get(n);
        if (wide != null) {
            return wide[i];
        }
        int half = n / 2;
        if (n % 2 == 1) {
            return (i - half) * 10;
        }
        return (i < half ? i - half : i - half + 1) * 10;
    }

    static P place(int ox, int oy, int dx, int dy, String facing) {
        return switch (facing) {
            case "east" -> new P(ox + dx, oy + dy);
            case "west" -> new P(ox - dx, oy + dy);
            case "north" -> new P(ox + dy, oy - dx);
            default -> new P(ox + dy, oy + dx);
        };
    }

    static Map<String, String> attrs(String... keyValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    static final class Circ {
        final List<String> libs;
        final List<Comp> comps = new ArrayList<>();
        final List<Wire> wires = new ArrayList<>();
        final List<SubCircuit> extraCircuits = new ArrayList<>();

        Circ(String... libs) {
            this.libs = libs.length == 0 ? List.of("#Wiring", "#Gates") : List.of(libs);
        }

        String lib(String desc) {
            return String.valueOf(libs.indexOf(desc));
        }

        void comp(String lib, String name, int x, int y, Map<String, String> attrs) {
            comps.add(new Comp(lib, name, x, y, attrs));
        }

        void pin(int x, int y, String label) {
            pin(x, y, label, false, null, null);
        }

        void output(int x, int y, String label, String facing) {
            pin(x, y, label, true, facing, null);
        }

        void pin(int x, int y, String label, boolean output, String facing, Integer width) {
            Map<String, String> a = new LinkedHashMap<>();
            if (facing != null) a.put("facing", facing);
            if (output) a.put("output", "true");
            if (label != null) a.put("label", label);
            if (width != null) a.put("width", String.valueOf(width));
            comp(lib("#Wiring"), "Pin", x, y, a);
        }

        void wire(int x1, int y1, int x2, int y2) {
            wire(new P(x1, y1), new P(x2, y2));
        }

        void wire(P a, P b) {
            if (!a.equals(b)) {
                wires.add(new Wire(a, b));
            }
        }

        Gate gate(String name, int x, int y) {
            return gate(name, x, y, null, null, "east", List.of(), null);
        }

        Gate gate(String name, int x, int y, Integer size, Integer inputs, String facing,
                  List<Integer> negate, String xor) {
            Map<String, String> a = new LinkedHashMap<>();
            if (!facing.equals("east")) a.put("facing", facing);
            if (size != null) a.put("size", String.valueOf(size));
            if (inputs != null) a.put("inputs", String.valueOf(inputs));
            if (xor != null) a.put("xor", xor);
            for (int i : negate) a.put("negate" + i, "true");
            comp(lib("#Gates"), name, x, y, a);
            int s = size == null ? 50 : size;
            int n = inputs == null ? 2 : inputs;
            List<P> ports = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                int dx = -(s + EXTRA.get(name) + (negate.contains(i) ? 10 : 0));
                ports.add(place(x, y, dx, dy(s, n, i), facing));
            }
            return new Gate(ports, new P(x, y));
        }

        Gate unary(String name, int x, int y, String facing, Integer size) {
            Map<String, String> a = new LinkedHashMap<>();
            if (!facing.equals("east")) a.put("facing", facing);
            if (size != null) a.put("size", String.valueOf(size));
            comp(lib("#Gates"), name, x, y, a);
            int width = name.equals("Buffer") ? 20 : (size == null ? 30 : size);
            return new Gate(List.of(place(x, y, -width, 0, facing)), new P(x, y));
        }

        /** Connects each port with a straight stub to its own pin, or to the given source. */
        void ownInputs(List<P> ports, String facing, List<String> labels, List<Consumer<P>> sources) {
            P out = OUTWARD.get(facing);
            for (int k = 0; k < ports.size(); k++) {
                P port = ports.get(k);
                P end = new P(port.x() + out.x() * 30, port.y() + out.y() * 30);
                wire(end, port);
                if (sources != null && sources.get(k) != null) {
                    sources.get(k).accept(end);
                } else {
                    pin(end.x(), end.y(), labels.get(k));
                }
            }
        }

        void ownInputs(List<P> ports, String facing, String... labels) {
            ownInputs(ports, facing, List.of(labels), null);
        }

        void ownOutput(P out, String facing, String label) {
            P o = OUTWARD.get(facing);
            P end = new P(out.x() - o.x() * 30, out.y() - o.y() * 30);
            wire(out, end);
            pin(end.x(), end.y(), label, true, OPPOSITE.get(facing), null);
        }

        String xml() {
            List<String> lines = new ArrayList<>();
            lines.add("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
            lines.add("<project source=\"3.7.2\" version=\"1.0\">");
            for (int i = 0; i < libs.size(); i++) {
                lines.add("  <lib desc=\"" + libs.get(i) + "\" name=\"" + i + "\"/>");
            }
            lines.add("  <main name=\"main\"/>");
            lines.addAll(circuitXml("main", comps, wires));
            for (SubCircuit sub : extraCircuits) {
                lines.addAll(circuitXml(sub.name(), sub.comps(), sub.wires()));
            }
            lines.add("</project>");
            return String.join("\n", lines) + "\n";
        }

        static List<String> circuitXml(String name, List<Comp> comps, List<Wire> wires) {
            List<String> lines = new ArrayList<>();
            lines.add("  <circuit name=\"" + name + "\">");
            lines.add("    <a name=\"circuit\" val=\"" + name + "\"/>");
            for (Comp c : comps) {
                String lib = c.lib() == null ? "" : " lib=\"" + c.lib() + "\"";
                StringBuilder body = new StringBuilder();
                c.attrs().forEach((k, v) -> body.append("<a name=\"").append(k).append("\" val=\"")
                        .append(v).append("\"/>"));
                lines.add("    <comp" + lib + " loc=\"(" + c.x() + "," + c.y() + ")\" name=\"" + c.name()
                        + "\">" + body + "</comp>");
            }
            for (Wire w : wires) {
                lines.add("    <wire from=\"(" + w.a().x() + "," + w.a().y() + ")\" to=\"(" + w.b().x()
                        + "," + w.b().y() + ")\"/>");
            }
            lines.add("  </circuit>");
            return lines;
        }
    }

    static void save(String name, Circ circ) throws IOException {
        Files.writeString(OUT.resolve(name + ".circ"), circ.xml());
    }

    static void halfAdder() throws IOException {
        Circ c = new Circ();
        c.pin(100, 100, "A");
        c.pin(100, 200, "B");
        c.gate("XOR Gate", 300, 120);
        c.gate("AND Gate", 300, 220);
        c.output(400, 120, "Sum", "west");
        c.output(400, 220, "Cout", "west");
        c.wire(100, 100, 240, 100);
        c.wire(160, 100, 160, 240);
        c.wire(160, 240, 250, 240);
        c.wire(100, 200, 250, 200);
        c.wire(200, 200, 200, 140);
        c.wire(200, 140, 240, 140);
        c.wire(300, 120, 400, 120);
        c.wire(300, 220, 400, 220);
        save("half_adder", c);
    }

    static void fullAdder() throws IOException {
        Circ c = new Circ();
        c.pin(100, 120, "A");
        c.pin(100, 170, "B");
        c.pin(100, 320, "Cin");
        c.gate("XOR Gate", 300, 130, 30, null, "east", List.of(), null);
        c.gate("XOR Gate", 420, 150, 30, null, "east", List.of(), null);
        c.gate("AND Gate", 300, 230, 30, null, "east", List.of(), null);
        c.gate("AND Gate", 420, 270, 30, null, "east", List.of(), null);
        c.gate("OR Gate", 520, 250, 30, null, "east", List.of(), null);
        c.output(560, 150, "Sum", "west");
        c.output(560, 250, "Cout", "west");
        int[][] wires = {
                {100, 120, 260, 120}, {150, 120, 150, 220}, {150, 220, 270, 220},
                {100, 170, 200, 170}, {200, 170, 200, 140}, {200, 140, 260, 140},
                {200, 170, 200, 240}, {200, 240, 270, 240},
                {300, 130, 340, 130}, {340, 130, 340, 140}, {340, 140, 380, 140},
                {340, 140, 340, 260}, {340, 260, 390, 260},
                {100, 320, 360, 320}, {360, 320, 360, 160}, {360, 160, 380, 160},
                {360, 280, 390, 280},
                {300, 230, 460, 230}, {460, 230, 460, 240}, {460, 240, 490, 240},
                {420, 270, 460, 270}, {460, 270, 460, 260}, {460, 260, 490, 260},
                {420, 150, 560, 150}, {520, 250, 560, 250}};
        for (int[] w : wires) c.wire(w[0], w[1], w[2], w[3]);
        save("full_adder", c);
    }

    static void mux2() throws IOException {
        Circ c = new Circ();
        c.pin(100, 100, "S");
        c.pin(100, 140, "D0");
        c.pin(100, 200, "D1");
        c.unary("NOT Gate", 200, 100, "east", null);
        c.gate("AND Gate", 320, 120);
        c.gate("AND Gate", 320, 220);
        c.gate("OR Gate", 440, 170);
        c.output(500, 170, "Y", "west");
        int[][] wires = {
                {100, 100, 170, 100}, {130, 100, 130, 240}, {130, 240, 270, 240},
                {200, 100, 270, 100}, {100, 140, 270, 140}, {100, 200, 270, 200},
                {320, 120, 350, 120}, {350, 120, 350, 150}, {350, 150, 390, 150},
                {320, 220, 350, 220}, {350, 220, 350, 190}, {350, 190, 390, 190},
                {440, 170, 500, 170}};
        for (int[] w : wires) c.wire(w[0], w[1], w[2], w[3]);
        save("mux2", c);
    }

    static void xorFromNand() throws IOException {
        Circ c = new Circ();
        c.pin(100, 90, "A");
        c.pin(100, 230, "B");
        c.gate("NAND Gate", 260, 150);
        c.gate("NAND Gate", 400, 110);
        c.gate("NAND Gate", 400, 210);
        c.gate("NAND Gate", 520, 160);
        c.output(580, 160, "Y", "west");
        int[][] wires = {
                {100, 90, 340, 90}, {150, 90, 150, 130}, {150, 130, 200, 130},
                {100, 230, 340, 230}, {170, 230, 170, 170}, {170, 170, 200, 170},
                {260, 150, 300, 150}, {300, 150, 300, 130}, {300, 130, 340, 130},
                {300, 150, 300, 190}, {300, 190, 340, 190},
                {400, 110, 430, 110}, {430, 110, 430, 140}, {430, 140, 460, 140},
                {400, 210, 430, 210}, {430, 210, 430, 180}, {430, 180, 460, 180},
                {520, 160, 580, 160}};
        for (int[] w : wires) c.wire(w[0], w[1], w[2], w[3]);
        save("xor_from_nand", c);
    }

    record GateCase(String name, Integer size, Integer inputs, String xor, String signals) {
    }

    static void gateSizes() throws IOException {
        Circ c = new Circ();
        String signals = "ABCDE";
        List<GateCase> gates = List.of(
                new GateCase("AND Gate", 30, 3, null, "ABC"), new GateCase("OR Gate", 30, 4, null, "ABCD"),
                new GateCase("NAND Gate", 30, 5, null, "ABCDE"), new GateCase("NOR Gate", 50, 3, null, "ABC"),
                new GateCase("XOR Gate", 50, 4, "odd", "ABCD"), new GateCase("XNOR Gate", 50, 5, "odd", "ABCDE"),
                new GateCase("AND Gate", 70, 2, null, "AB"), new GateCase("OR Gate", 70, 3, null, "ABC"),
                new GateCase("NAND Gate", 70, 4, null, "ABCD"), new GateCase("XOR Gate", 70, 3, null, "ABC"),
                new GateCase("XNOR Gate", 30, 2, null, "AB"), new GateCase("AND Gate", 50, 6, null, "ABCDEA"),
                new GateCase("NOR Gate", 70, 8, null, "ABCDEABC"), new GateCase("XOR Gate", 30, 5, null, "ABCDE"),
                new GateCase("XNOR Gate", 70, 3, null, "BCD"), new GateCase("OR Gate", 50, null, null, "DE"));
        int yMax = 150 + 110 * gates.size();
        for (int i = 0; i < signals.length(); i++) {
            int x = 100 + 40 * i;
            c.pin(x, 40, String.valueOf(signals.charAt(i)), false, "south", null);
            c.wire(x, 40, x, yMax);
        }
        for (int k = 0; k < gates.size(); k++) {
            GateCase g = gates.get(k);
            int y = 150 + 110 * k;
            Gate placed = c.gate(g.name(), 600, y, g.size(), g.inputs(), "east", List.of(), g.xor());
            for (int i = 0; i < placed.ports().size(); i++) {
                P port = placed.ports().get(i);
                c.wire(new P(100 + 40 * signals.indexOf(g.signals().charAt(i)), port.y()), port);
            }
            c.wire(placed.out(), new P(640, y));
            c.output(640, y, "O" + (k + 1), "west");
        }
        save("gate_sizes", c);
    }

    static void rotated() throws IOException {
        Circ c = new Circ();
        Gate g = c.gate("AND Gate", 200, 150, null, null, "north", List.of(), null);
        c.ownInputs(g.ports(), "north", "r1a", "r1b");
        c.ownOutput(g.out(), "north", "Q1");
        g = c.gate("OR Gate", 400, 250, null, 3, "south", List.of(), null);
        c.ownInputs(g.ports(), "south", "r2a", "r2b", "r2c");
        c.ownOutput(g.out(), "south", "Q2");
        g = c.gate("XNOR Gate", 200, 400, null, null, "west", List.of(), null);
        c.ownInputs(g.ports(), "west", "r3a", "r3b");
        c.ownOutput(g.out(), "west", "Q3");
        g = c.unary("NOT Gate", 400, 400, "north", null);
        c.ownInputs(g.ports(), "north", "r4");
        c.ownOutput(g.out(), "north", "Q4");
        g = c.unary("Buffer", 500, 400, "south", null);
        c.ownInputs(g.ports(), "south", "r5");
        c.ownOutput(g.out(), "south", "Q5");
        g = c.unary("NOT Gate", 600, 400, "west", 20);
        c.ownInputs(g.ports(), "west", "r6");
        c.ownOutput(g.out(), "west", "Q6");
        save("rotated", c);
    }

    static void negatedInputs() throws IOException {
        Circ c = new Circ();
        Gate g = c.gate("AND Gate", 200, 100, null, null, "east", List.of(0), null);
        c.ownInputs(g.ports(), "east", "n1a", "n1b");
        c.ownOutput(g.out(), "east", "N1");
        g = c.gate("OR Gate", 300, 250, null, 3, "west", List.of(0), null);
        c.ownInputs(g.ports(), "west", "n2a", "n2b", "n2c");
        c.ownOutput(g.out(), "west", "N2");
        g = c.gate("NAND Gate", 500, 100, null, 3, "north", List.of(2), null);
        c.ownInputs(g.ports(), "north", "n3a", "n3b", "n3c");
        c.ownOutput(g.out(), "north", "N3");
        g = c.gate("XOR Gate", 700, 250, null, null, "south", List.of(1), null);
        c.ownInputs(g.ports(), "south", "n4a", "n4b");
        c.ownOutput(g.out(), "south", "N4");
        save("negated_inputs", c);
    }

    static void constants() throws IOException {
        Circ c = new Circ();
        String w = c.lib("#Wiring");
        Consumer<P> constOne = p -> c.comp(w, "Constant", p.x(), p.y(), attrs());
        Consumer<P> constZero = p -> c.comp(w, "Constant", p.x(), p.y(), attrs("value", "0x0"));
        Consumer<P> power = p -> c.comp(w, "Power", p.x(), p.y(), attrs());
        Consumer<P> ground = p -> c.comp(w, "Ground", p.x(), p.y(), attrs());
        List<String> none = new ArrayList<>();
        none.add(null);
        Gate g = c.gate("AND Gate", 200, 100);
        c.ownInputs(g.ports(), "east", java.util.Arrays.asList("k1", null), java.util.Arrays.asList(null, constOne));
        c.ownOutput(g.out(), "east", "Y1");
        g = c.gate("OR Gate", 200, 200);
        c.ownInputs(g.ports(), "east", java.util.Arrays.asList("k2", null), java.util.Arrays.asList(null, constZero));
        c.ownOutput(g.out(), "east", "Y2");
        g = c.gate("XOR Gate", 200, 300);
        c.ownInputs(g.ports(), "east", java.util.Arrays.asList("k3", null), java.util.Arrays.asList(null, power));
        c.ownOutput(g.out(), "east", "Y3");
        g = c.gate("NOR Gate", 200, 400);
        c.ownInputs(g.ports(), "east", java.util.Arrays.asList("k4", null), java.util.Arrays.asList(null, ground));
        c.ownOutput(g.out(), "east", "Y4");
        g = c.unary("NOT Gate", 200, 500, "east", null);
        c.ownInputs(g.ports(), "east", none, List.of(constZero));
        c.ownOutput(g.out(), "east", "Y5");
        save("constants", c);
    }

    static void unsupported() throws IOException {
        Circ c = new Circ("#Wiring", "#Gates", "#Memory", "#Plexers", "#I/O", "#Base");
        c.pin(100, 100, "A");
        c.pin(100, 140, "B");
        c.gate("AND Gate", 250, 120);
        c.output(300, 120, "Y", "west");
        c.wire(100, 100, 200, 100);
        c.wire(100, 140, 200, 140);
        c.wire(250, 120, 300, 120);
        c.comp(c.lib("#Memory"), "D Flip-Flop", 400, 300, attrs());
        c.comp(c.lib("#Plexers"), "Multiplexer", 500, 300, attrs());
        c.comp(c.lib("#I/O"), "LED", 600, 300, attrs());
        c.comp(c.lib("#Wiring"), "Tunnel", 400, 400, attrs("label", "t"));
        c.comp(c.lib("#Wiring"), "Clock", 500, 400, attrs());
        c.comp(c.lib("#Wiring"), "Probe", 300, 200, attrs());
        c.comp(c.lib("#Base"), "Text", 100, 400, attrs("text", "ignored note"));
        c.comp(null, "helper", 600, 400, attrs());
        c.extraCircuits.add(new SubCircuit("helper",
                List.of(new Comp("0", "Pin", 100, 100, attrs("label", "h"))), List.of()));
        save("unsupported", c);
    }

    static void floating() throws IOException {
        Circ c = new Circ();
        c.pin(150, 80, "A");
        c.pin(150, 120, "B");
        c.gate("AND Gate", 300, 100);
        c.output(350, 100, "Y", "west");
        c.output(350, 200, "Z", "west");
        c.wire(150, 80, 250, 80);
        c.wire(150, 120, 240, 120);
        c.wire(300, 100, 350, 100);
        save("floating", c);
    }

    static void unlabelledPins() throws IOException {
        Circ c = new Circ();
        c.pin(100, 100, null);
        c.pin(100, 200, null);
        c.pin(100, 300, "IN1");
        c.gate("AND Gate", 300, 200, null, 3, "east", List.of(), null);
        c.output(400, 200, null, "west");
        int[][] wires = {
                {100, 100, 200, 100}, {200, 100, 200, 180}, {200, 180, 250, 180},
                {100, 200, 250, 200},
                {100, 300, 200, 300}, {200, 300, 200, 220}, {200, 220, 250, 220},
                {300, 200, 400, 200}};
        for (int[] w : wires) c.wire(w[0], w[1], w[2], w[3]);
        save("unlabelled_pins", c);
    }

    static void duplicateLabels() throws IOException {
        Circ c = new Circ();
        c.pin(100, 100, "A");
        c.pin(100, 200, "a");
        c.gate("OR Gate", 300, 150);
        c.output(400, 150, "Y", "west");
        int[][] wires = {
                {100, 100, 200, 100}, {200, 100, 200, 130}, {200, 130, 250, 130},
                {100, 200, 200, 200}, {200, 200, 200, 170}, {200, 170, 250, 170},
                {300, 150, 400, 150}};
        for (int[] w : wires) c.wire(w[0], w[1], w[2], w[3]);
        save("duplicate_labels", c);
    }

    static void contention() throws IOException {
        Circ c = new Circ();
        c.pin(100, 100, "A");
        c.pin(100, 200, "B");
        c.output(300, 150, "Y", "west");
        c.wire(100, 100, 200, 100);
        c.wire(100, 200, 200, 200);
        c.wire(200, 100, 200, 200);
        c.wire(200, 150, 300, 150);
        save("contention", c);
    }

    static void multibit() throws IOException {
        Circ c = new Circ();
        c.pin(100, 100, "A", false, null, 4);
        c.pin(300, 100, "Y", true, "west", 4);
        c.wire(100, 100, 300, 100);
        save("multibit", c);
    }

    public static void main(String[] args) throws IOException {
        Files.createDirectories(OUT);
        halfAdder();
        fullAdder();
        mux2();
        xorFromNand();
        gateSizes();
        rotated();
        negatedInputs();
        constants();
        unsupported();
        floating();
        unlabelledPins();
        duplicateLabels();
        contention();
        multibit();
        try (var files = Files.list(OUT)) {
            files.map(Path::getFileName).sorted().forEach(System.out::println);
        }
    }
}
