package interface_adapter.check_circuit;

import entity.truthtable.TruthTableTextFormat;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecTemplateTest {

    @Test
    void listsEveryRowForSmallCircuits() {
        String template = SpecTemplate.forLabels(List.of("A", "B"), List.of("Cout", "Sum"));

        assertEquals("# Replace each ? with the output you expect: 0, 1, or X for \"don't care\".\n"
                + "inputs: A, B\n"
                + "outputs: Cout, Sum\n"
                + "0 0 | ? ?\n"
                + "0 1 | ? ?\n"
                + "1 0 | ? ?\n"
                + "1 1 | ? ?\n", template);
    }

    @Test
    void filledInTemplateParses() throws Exception {
        String filled = SpecTemplate.forLabels(List.of("A"), List.of("Y")).replace("?", "1");

        assertEquals(2, new TruthTableTextFormat().parse(filled).rows().size());
    }

    @Test
    void largeCircuitsGetOnlyTheHeader() {
        String template = SpecTemplate.forLabels(Collections.nCopies(7, "I"), List.of("Y"));

        assertEquals(3, template.split("\n").length);
        assertTrue(template.endsWith("outputs: Y\n"));
    }
}
