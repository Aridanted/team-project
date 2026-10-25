import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Probe {
    static ComponentFactory factory(String cls) throws Exception {
        Class<?> c = Class.forName(cls);
        try {
            java.lang.reflect.Field field = c.getDeclaredField("FACTORY");
            field.setAccessible(true);
            return (ComponentFactory) field.get(null);
        } catch (NoSuchFieldException e) {
            java.lang.reflect.Constructor<?> ctor = c.getDeclaredConstructor();
            ctor.setAccessible(true);
            return (ComponentFactory) ctor.newInstance();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static String ends(ComponentFactory f, Map<String, String> attrValues) {
        AttributeSet attrs = f.createAttributeSet();
        for (Map.Entry<String, String> e : attrValues.entrySet()) {
            Attribute a = null;
            for (Attribute<?> x : attrs.getAttributes()) if (x.getName().equals(e.getKey())) a = x;
            if (a == null) return "NO ATTR " + e.getKey();
            attrs.setValue(a, a.parse(e.getValue()));
        }
        Component c = f.createComponent(Location.create(500, 500), attrs);
        List<String> out = new ArrayList<>();
        for (EndData d : c.getEnds()) {
            Location l = d.getLocation();
            out.add((d.isInput() && d.isOutput() ? "io" : d.isInput() ? "in" : "out")
                    + "(" + (l.getX() - 500) + "," + (l.getY() - 500) + ")w" + d.getWidth().getWidth());
        }
        return String.join(" ", out);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void defaults(String label, ComponentFactory f) {
        AttributeSet attrs = f.createAttributeSet();
        StringBuilder sb = new StringBuilder(label + " name=\"" + f.getName() + "\" defaults:");
        for (Attribute a : attrs.getAttributes()) {
            Object v = attrs.getValue(a);
            sb.append(" ").append(a.getName()).append("=").append(v == null ? "null" : a.toStandardString(v));
        }
        System.out.println(sb);
    }

    static Map<String, String> m(String... kv) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) map.put(kv[i], kv[i + 1]);
        return map;
    }

    public static void main(String[] args) throws Exception {
        String g = "com.cburch.logisim.std.gates.";
        String w = "com.cburch.logisim.std.wiring.";
        String[] gates = {"AndGate", "OrGate", "NandGate", "NorGate", "XorGate", "XnorGate"};
        for (String cls : gates) defaults(cls, factory(g + cls));
        for (String cls : new String[]{"NotGate", "Buffer", "OddParityGate"}) defaults(cls, factory(g + cls));
        for (String cls : new String[]{"Pin", "Constant", "Power", "Ground", "Tunnel", "Clock", "Probe"}) defaults(cls, factory(w + cls));

        System.out.println("\n== multi-input gates, facing east ==");
        for (String cls : gates) {
            ComponentFactory f = factory(g + cls);
            System.out.println(cls + " default: " + ends(f, m()));
            for (String size : new String[]{"30", "50", "70"})
                for (String n : new String[]{"2", "3", "4", "5", "6", "7", "8", "9"})
                    System.out.println(cls + " size=" + size + " inputs=" + n + ": " + ends(f, m("size", size, "inputs", n)));
        }

        System.out.println("\n== facings (AND / XOR / NAND) ==");
        for (String cls : new String[]{"AndGate", "XorGate", "NandGate"})
            for (String facing : new String[]{"east", "west", "north", "south"})
                for (String n : new String[]{"2", "3"})
                    System.out.println(cls + " facing=" + facing + " inputs=" + n + ": "
                            + ends(factory(g + cls), m("facing", facing, "inputs", n)));

        System.out.println("\n== negated inputs ==");
        for (String facing : new String[]{"east", "west", "north", "south"}) {
            System.out.println("AND facing=" + facing + " negate0: " + ends(factory(g + "AndGate"), m("facing", facing, "negate0", "true")));
            System.out.println("AND facing=" + facing + " negate1: " + ends(factory(g + "AndGate"), m("facing", facing, "negate1", "true")));
            System.out.println("XNOR facing=" + facing + " size=30 negate0: " + ends(factory(g + "XnorGate"), m("facing", facing, "size", "30", "negate0", "true")));
        }

        System.out.println("\n== NOT / Buffer ==");
        for (String facing : new String[]{"east", "west", "north", "south"}) {
            System.out.println("NOT facing=" + facing + ": " + ends(factory(g + "NotGate"), m("facing", facing)));
            System.out.println("NOT facing=" + facing + " size=20: " + ends(factory(g + "NotGate"), m("facing", facing, "size", "20")));
            System.out.println("Buffer facing=" + facing + ": " + ends(factory(g + "Buffer"), m("facing", facing)));
        }

        System.out.println("\n== Pins / constants ==");
        for (String facing : new String[]{"east", "west", "north", "south"}) {
            System.out.println("Pin in facing=" + facing + ": " + ends(factory(w + "Pin"), m("facing", facing)));
            System.out.println("Pin out facing=" + facing + ": " + ends(factory(w + "Pin"), m("facing", facing, "output", "true")));
            System.out.println("Constant facing=" + facing + ": " + ends(factory(w + "Constant"), m("facing", facing)));
            System.out.println("Power facing=" + facing + ": " + ends(factory(w + "Power"), m("facing", facing)));
            System.out.println("Ground facing=" + facing + ": " + ends(factory(w + "Ground"), m("facing", facing)));
        }
    }
}
