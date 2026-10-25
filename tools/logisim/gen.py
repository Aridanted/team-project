"""Generates source .circ fixtures. They are then re-saved by Logisim itself (Normalize.java)."""
import os

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "build")
EXTRA = {"AND Gate": 0, "OR Gate": 0, "NAND Gate": 10, "NOR Gate": 10, "XOR Gate": 10, "XNOR Gate": 20}
WIDE = {50: {2: [-20, 20], 3: [-20, 0, 20]}, 70: {2: [-20, 20], 3: [-30, 0, 30], 4: [-30, -10, 10, 30]}}
OUTWARD = {"east": (-1, 0), "west": (1, 0), "north": (0, 1), "south": (0, -1)}


def dy_of(size, n, i):
    if size in WIDE and n in WIDE[size]:
        return WIDE[size][n][i]
    half = n // 2
    if n % 2 == 1:
        return (i - half) * 10
    return (i - half if i < half else i - half + 1) * 10


def place(ox, oy, dx, dy, facing):
    return {"east": (ox + dx, oy + dy), "west": (ox - dx, oy + dy),
            "north": (ox + dy, oy - dx), "south": (ox + dy, oy + dx)}[facing]


class Circ:
    def __init__(self, libs=("#Wiring", "#Gates")):
        self.libs = list(libs)
        self.comps = []
        self.wires = []
        self.extra_circuits = []

    def lib(self, desc):
        return str(self.libs.index(desc))

    def comp(self, lib, name, x, y, **attrs):
        self.comps.append((lib, name, x, y, attrs))

    def pin(self, x, y, label=None, output=False, facing=None, width=None):
        attrs = {}
        if facing: attrs["facing"] = facing
        if output: attrs["output"] = "true"
        if label is not None: attrs["label"] = label
        if width: attrs["width"] = str(width)
        self.comp(self.lib("#Wiring"), "Pin", x, y, **attrs)

    def wire(self, a, b):
        if a != b:
            self.wires.append((a, b))

    def gate(self, name, x, y, size=None, inputs=None, facing="east", negate=(), xor=None):
        attrs = {}
        if facing != "east": attrs["facing"] = facing
        if size: attrs["size"] = str(size)
        if inputs: attrs["inputs"] = str(inputs)
        if xor: attrs["xor"] = xor
        for i in negate: attrs["negate%d" % i] = "true"
        self.comp(self.lib("#Gates"), name, x, y, **attrs)
        s, n = size or 50, inputs or 2
        ports = []
        for i in range(n):
            dx = -(s + EXTRA[name] + (10 if i in negate else 0))
            ports.append(place(x, y, dx, dy_of(s, n, i), facing))
        return ports, (x, y)

    def unary(self, name, x, y, facing="east", size=None):
        attrs = {}
        if facing != "east": attrs["facing"] = facing
        if size: attrs["size"] = str(size)
        self.comp(self.lib("#Gates"), name, x, y, **attrs)
        width = 20 if name == "Buffer" else (size or 30)
        return [place(x, y, -width, 0, facing)], (x, y)

    def own_inputs(self, ports, facing, labels, length=30, sources=None):
        """Connects each port with a straight stub to its own pin (or a given source)."""
        ox, oy = OUTWARD[facing]
        for k, (port, label) in enumerate(zip(ports, labels)):
            end = (port[0] + ox * length, port[1] + oy * length)
            self.wire(end, port)
            if sources and sources[k]:
                sources[k](end)
            else:
                self.pin(end[0], end[1], label)

    def own_output(self, out, facing, label, length=30):
        ox, oy = OUTWARD[facing]
        end = (out[0] - ox * length, out[1] - oy * length)
        self.wire(out, end)
        self.pin(end[0], end[1], label, output=True, facing={"east": "west", "west": "east",
                                                          "north": "south", "south": "north"}[facing])

    def xml(self):
        lines = ['<?xml version="1.0" encoding="UTF-8" standalone="no"?>',
                 '<project source="3.7.2" version="1.0">']
        for i, desc in enumerate(self.libs):
            lines.append('  <lib desc="%s" name="%d"/>' % (desc, i))
        lines.append('  <main name="main"/>')
        lines += self.circuit_xml("main", self.comps, self.wires)
        for name, comps, wires in self.extra_circuits:
            lines += self.circuit_xml(name, comps, wires)
        lines.append("</project>")
        return "\n".join(lines) + "\n"

    @staticmethod
    def circuit_xml(name, comps, wires):
        lines = ['  <circuit name="%s">' % name, '    <a name="circuit" val="%s"/>' % name]
        for lib, cname, x, y, attrs in comps:
            lib_attr = ' lib="%s"' % lib if lib is not None else ""
            body = "".join('<a name="%s" val="%s"/>' % kv for kv in attrs.items())
            lines.append('    <comp%s loc="(%d,%d)" name="%s">%s</comp>' % (lib_attr, x, y, cname, body))
        for a, b in wires:
            lines.append('    <wire from="(%d,%d)" to="(%d,%d)"/>' % (a + b))
        lines.append("  </circuit>")
        return lines


def save(name, circ):
    with open(os.path.join(OUT, name + ".circ"), "w") as f:
        f.write(circ.xml())


def half_adder():
    c = Circ()
    c.pin(100, 100, "A"); c.pin(100, 200, "B")
    c.gate("XOR Gate", 300, 120)            # inputs (240,100) (240,140)
    c.gate("AND Gate", 300, 220)            # inputs (250,200) (250,240)
    c.pin(400, 120, "Sum", output=True, facing="west"); c.pin(400, 220, "Cout", output=True, facing="west")
    c.wire((100, 100), (240, 100))          # A to XOR in0
    c.wire((160, 100), (160, 240))          # T-junction on A's wire interior
    c.wire((160, 240), (250, 240))          # A to AND in1
    c.wire((100, 200), (250, 200))          # B to AND in0, crosses A's branch at (160,200)
    c.wire((200, 200), (200, 140))          # T-junction on B's wire interior
    c.wire((200, 140), (240, 140))          # B to XOR in1
    c.wire((300, 120), (400, 120)); c.wire((300, 220), (400, 220))
    save("half_adder", c)


def full_adder():
    c = Circ()
    c.pin(100, 120, "A"); c.pin(100, 170, "B"); c.pin(100, 320, "Cin")
    c.gate("XOR Gate", 300, 130, size=30)   # (260,120) (260,140)
    c.gate("XOR Gate", 420, 150, size=30)   # (380,140) (380,160)
    c.gate("AND Gate", 300, 230, size=30)   # (270,220) (270,240)
    c.gate("AND Gate", 420, 270, size=30)   # (390,260) (390,280)
    c.gate("OR Gate", 520, 250, size=30)    # (490,240) (490,260)
    c.pin(560, 150, "Sum", output=True, facing="west"); c.pin(560, 250, "Cout", output=True, facing="west")
    c.wire((100, 120), (260, 120)); c.wire((150, 120), (150, 220)); c.wire((150, 220), (270, 220))
    c.wire((100, 170), (200, 170)); c.wire((200, 170), (200, 140)); c.wire((200, 140), (260, 140))
    c.wire((200, 170), (200, 240)); c.wire((200, 240), (270, 240))
    c.wire((300, 130), (340, 130)); c.wire((340, 130), (340, 140)); c.wire((340, 140), (380, 140))
    c.wire((340, 140), (340, 260)); c.wire((340, 260), (390, 260))
    c.wire((100, 320), (360, 320)); c.wire((360, 320), (360, 160)); c.wire((360, 160), (380, 160))
    c.wire((360, 280), (390, 280))
    c.wire((300, 230), (460, 230)); c.wire((460, 230), (460, 240)); c.wire((460, 240), (490, 240))
    c.wire((420, 270), (460, 270)); c.wire((460, 270), (460, 260)); c.wire((460, 260), (490, 260))
    c.wire((420, 150), (560, 150)); c.wire((520, 250), (560, 250))
    save("full_adder", c)


def mux2():
    c = Circ()
    c.pin(100, 100, "S"); c.pin(100, 140, "D0"); c.pin(100, 200, "D1")
    c.unary("NOT Gate", 200, 100)           # input (170,100)
    c.gate("AND Gate", 320, 120)            # (270,100) (270,140)
    c.gate("AND Gate", 320, 220)            # (270,200) (270,240)
    c.gate("OR Gate", 440, 170)             # (390,150) (390,190)
    c.pin(500, 170, "Y", output=True, facing="west")
    c.wire((100, 100), (170, 100)); c.wire((130, 100), (130, 240)); c.wire((130, 240), (270, 240))
    c.wire((200, 100), (270, 100))
    c.wire((100, 140), (270, 140)); c.wire((100, 200), (270, 200))
    c.wire((320, 120), (350, 120)); c.wire((350, 120), (350, 150)); c.wire((350, 150), (390, 150))
    c.wire((320, 220), (350, 220)); c.wire((350, 220), (350, 190)); c.wire((350, 190), (390, 190))
    c.wire((440, 170), (500, 170))
    save("mux2", c)


def xor_from_nand():
    c = Circ()
    c.pin(100, 90, "A"); c.pin(100, 230, "B")
    c.gate("NAND Gate", 260, 150)           # (200,130) (200,170)
    c.gate("NAND Gate", 400, 110)           # (340,90) (340,130)
    c.gate("NAND Gate", 400, 210)           # (340,190) (340,230)
    c.gate("NAND Gate", 520, 160)           # (460,140) (460,180)
    c.pin(580, 160, "Y", output=True, facing="west")
    c.wire((100, 90), (340, 90)); c.wire((150, 90), (150, 130)); c.wire((150, 130), (200, 130))
    c.wire((100, 230), (340, 230)); c.wire((170, 230), (170, 170)); c.wire((170, 170), (200, 170))
    c.wire((260, 150), (300, 150)); c.wire((300, 150), (300, 130)); c.wire((300, 130), (340, 130))
    c.wire((300, 150), (300, 190)); c.wire((300, 190), (340, 190))
    c.wire((400, 110), (430, 110)); c.wire((430, 110), (430, 140)); c.wire((430, 140), (460, 140))
    c.wire((400, 210), (430, 210)); c.wire((430, 210), (430, 180)); c.wire((430, 180), (460, 180))
    c.wire((520, 160), (580, 160))
    save("xor_from_nand", c)


def gate_sizes():
    c = Circ()
    signals = ["A", "B", "C", "D", "E"]
    bus_x = {s: 100 + 40 * i for i, s in enumerate(signals)}
    gates = [  # name, size, inputs, xor, signals per input
        ("AND Gate", 30, 3, None, "ABC"), ("OR Gate", 30, 4, None, "ABCD"), ("NAND Gate", 30, 5, None, "ABCDE"),
        ("NOR Gate", 50, 3, None, "ABC"), ("XOR Gate", 50, 4, "odd", "ABCD"), ("XNOR Gate", 50, 5, "odd", "ABCDE"),
        ("AND Gate", 70, 2, None, "AB"), ("OR Gate", 70, 3, None, "ABC"), ("NAND Gate", 70, 4, None, "ABCD"),
        ("XOR Gate", 70, 3, None, "ABC"), ("XNOR Gate", 30, 2, None, "AB"), ("AND Gate", 50, 6, None, "ABCDEA"),
        ("NOR Gate", 70, 8, None, "ABCDEABC"), ("XOR Gate", 30, 5, None, "ABCDE"), ("XNOR Gate", 70, 3, None, "BCD"),
        ("OR Gate", 50, None, None, "DE"),
    ]
    y_max = 150 + 110 * len(gates)
    for s in signals:
        c.pin(bus_x[s], 40, s, facing="south")
        c.wire((bus_x[s], 40), (bus_x[s], y_max))
    for k, (name, size, n, xor, sigs) in enumerate(gates):
        y = 150 + 110 * k
        ports, out = c.gate(name, 600, y, size=size, inputs=n, xor=xor)
        for port, s in zip(ports, sigs):
            c.wire((bus_x[s], port[1]), port)
        c.wire(out, (640, y))
        c.pin(640, y, "O%d" % (k + 1), output=True, facing="west")
    save("gate_sizes", c)


def rotated():
    c = Circ()
    ports, out = c.gate("AND Gate", 200, 150, facing="north")
    c.own_inputs(ports, "north", ["r1a", "r1b"]); c.own_output(out, "north", "Q1")
    ports, out = c.gate("OR Gate", 400, 250, inputs=3, facing="south")
    c.own_inputs(ports, "south", ["r2a", "r2b", "r2c"]); c.own_output(out, "south", "Q2")
    ports, out = c.gate("XNOR Gate", 200, 400, facing="west")
    c.own_inputs(ports, "west", ["r3a", "r3b"]); c.own_output(out, "west", "Q3")
    ports, out = c.unary("NOT Gate", 400, 400, facing="north")
    c.own_inputs(ports, "north", ["r4"]); c.own_output(out, "north", "Q4")
    ports, out = c.unary("Buffer", 500, 400, facing="south")
    c.own_inputs(ports, "south", ["r5"]); c.own_output(out, "south", "Q5")
    ports, out = c.unary("NOT Gate", 600, 400, facing="west", size=20)
    c.own_inputs(ports, "west", ["r6"]); c.own_output(out, "west", "Q6")
    save("rotated", c)


def negated_inputs():
    c = Circ()
    ports, out = c.gate("AND Gate", 200, 100, negate=(0,))
    c.own_inputs(ports, "east", ["n1a", "n1b"]); c.own_output(out, "east", "N1")
    ports, out = c.gate("OR Gate", 300, 250, inputs=3, facing="west", negate=(0,))
    c.own_inputs(ports, "west", ["n2a", "n2b", "n2c"]); c.own_output(out, "west", "N2")
    ports, out = c.gate("NAND Gate", 500, 100, inputs=3, facing="north", negate=(2,))
    c.own_inputs(ports, "north", ["n3a", "n3b", "n3c"]); c.own_output(out, "north", "N3")
    ports, out = c.gate("XOR Gate", 700, 250, facing="south", negate=(1,))
    c.own_inputs(ports, "south", ["n4a", "n4b"]); c.own_output(out, "south", "N4")
    save("negated_inputs", c)


def constants():
    c = Circ()
    w = c.lib("#Wiring")
    const = lambda value=None: (lambda p: c.comp(w, "Constant", p[0], p[1], **({"value": value} if value else {})))
    fixed = lambda kind: (lambda p: c.comp(w, kind, p[0], p[1]))
    ports, out = c.gate("AND Gate", 200, 100)
    c.own_inputs(ports, "east", ["k1", None], sources=[None, const()]); c.own_output(out, "east", "Y1")
    ports, out = c.gate("OR Gate", 200, 200)
    c.own_inputs(ports, "east", ["k2", None], sources=[None, const("0x0")]); c.own_output(out, "east", "Y2")
    ports, out = c.gate("XOR Gate", 200, 300)
    c.own_inputs(ports, "east", ["k3", None], sources=[None, fixed("Power")]); c.own_output(out, "east", "Y3")
    ports, out = c.gate("NOR Gate", 200, 400)
    c.own_inputs(ports, "east", ["k4", None], sources=[None, fixed("Ground")]); c.own_output(out, "east", "Y4")
    ports, out = c.unary("NOT Gate", 200, 500)
    c.own_inputs(ports, "east", [None], sources=[const("0x0")]); c.own_output(out, "east", "Y5")
    save("constants", c)


def unsupported():
    c = Circ(libs=("#Wiring", "#Gates", "#Memory", "#Plexers", "#I/O", "#Base"))
    c.pin(100, 100, "A"); c.pin(100, 140, "B")
    c.gate("AND Gate", 250, 120)
    c.pin(300, 120, "Y", output=True, facing="west")
    c.wire((100, 100), (200, 100)); c.wire((100, 140), (200, 140)); c.wire((250, 120), (300, 120))
    c.comp(c.lib("#Memory"), "D Flip-Flop", 400, 300)
    c.comp(c.lib("#Plexers"), "Multiplexer", 500, 300)
    c.comp(c.lib("#I/O"), "LED", 600, 300)
    c.comp(c.lib("#Wiring"), "Tunnel", 400, 400, label="t")
    c.comp(c.lib("#Wiring"), "Clock", 500, 400)
    c.comp(c.lib("#Wiring"), "Probe", 300, 200)
    c.comp(c.lib("#Base"), "Text", 100, 400, text="ignored note")
    c.comp(None, "helper", 600, 400)
    c.extra_circuits.append(("helper", [("0", "Pin", 100, 100, {"label": "h"})], []))
    save("unsupported", c)


def floating():
    c = Circ()
    c.pin(150, 80, "A"); c.pin(150, 120, "B")
    c.gate("AND Gate", 300, 100)            # (250,80) (250,120)
    c.pin(350, 100, "Y", output=True, facing="west")
    c.pin(350, 200, "Z", output=True, facing="west")
    c.wire((150, 80), (250, 80))
    c.wire((150, 120), (240, 120))          # stops 10 px short of the AND's second input
    c.wire((300, 100), (350, 100))
    save("floating", c)


def unlabelled_pins():
    c = Circ()
    c.pin(100, 100); c.pin(100, 200); c.pin(100, 300, "IN1")
    c.gate("AND Gate", 300, 200, inputs=3)  # (250,180) (250,200) (250,220)
    c.pin(400, 200, output=True, facing="west")
    c.wire((100, 100), (200, 100)); c.wire((200, 100), (200, 180)); c.wire((200, 180), (250, 180))
    c.wire((100, 200), (250, 200))
    c.wire((100, 300), (200, 300)); c.wire((200, 300), (200, 220)); c.wire((200, 220), (250, 220))
    c.wire((300, 200), (400, 200))
    save("unlabelled_pins", c)


def duplicate_labels():
    c = Circ()
    c.pin(100, 100, "A"); c.pin(100, 200, "a")
    c.gate("OR Gate", 300, 150)             # (250,130) (250,170)
    c.pin(400, 150, "Y", output=True, facing="west")
    c.wire((100, 100), (200, 100)); c.wire((200, 100), (200, 130)); c.wire((200, 130), (250, 130))
    c.wire((100, 200), (200, 200)); c.wire((200, 200), (200, 170)); c.wire((200, 170), (250, 170))
    c.wire((300, 150), (400, 150))
    save("duplicate_labels", c)


def contention():
    c = Circ()
    c.pin(100, 100, "A"); c.pin(100, 200, "B")
    c.pin(300, 150, "Y", output=True, facing="west")
    c.wire((100, 100), (200, 100)); c.wire((100, 200), (200, 200)); c.wire((200, 100), (200, 200))
    c.wire((200, 150), (300, 150))
    save("contention", c)


def multibit():
    c = Circ()
    c.pin(100, 100, "A", width=4)
    c.pin(300, 100, "Y", output=True, facing="west", width=4)
    c.wire((100, 100), (300, 100))
    save("multibit", c)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for fixture in (half_adder, full_adder, mux2, xor_from_nand, gate_sizes, rotated, negated_inputs,
                    constants, unsupported, floating, unlabelled_pins, duplicate_labels, contention, multibit):
        fixture()
    print("\n".join(sorted(os.listdir(OUT))))
