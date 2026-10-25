package entity.circuit;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LabelOrderTest {

    private static List<String> sorted(String... labels) {
        List<String> list = new ArrayList<>(List.of(labels));
        list.sort(LabelOrder.NATURAL);
        return list;
    }

    @Test
    void comparesDigitRunsByValue() {
        assertEquals(List.of("IN1", "IN2", "IN10"), sorted("IN10", "IN2", "IN1"));
    }

    @Test
    void ignoresCaseFirst() {
        assertEquals(List.of("a", "B", "cin"), sorted("cin", "B", "a"));
    }

    @Test
    void shorterPrefixComesFirst() {
        assertEquals(List.of("A", "A0", "A1", "B"), sorted("B", "A1", "A", "A0"));
    }

    @Test
    void leadingZerosDoNotChangeValueOrder() {
        assertEquals(List.of("x2", "x010"), sorted("x010", "x2"));
    }
}
