package data_access.llm.tutor;

import data_access.json.CircuitJsonCodec;
import entity.circuit.Circuit;
import entity.circuit.CircuitTestFactory;
import entity.design.CircuitDescriber;
import entity.simulation.CircuitSimulator;
import entity.truthtable.CheckResult;
import entity.truthtable.TruthTableChecker;
import entity.truthtable.TruthTableTextFormat;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TutorPromptBuilderTest {

    private final CircuitDescriber describer = new CircuitDescriber();
    private final CircuitJsonCodec codec = new CircuitJsonCodec();
    private final TutorPromptBuilder builder = new TutorPromptBuilder(describer, codec);

    private static CheckResult check(Circuit circuit, String spec) throws Exception {
        return new TruthTableChecker(new CircuitSimulator()).check(circuit, new TruthTableTextFormat().parse(spec));
    }

    @Test
    void withoutCircuitAsksForGeneralHelp() {
        assertEquals(TutorPromptBuilder.RULES + "\n\n" + TutorPromptBuilder.NO_CIRCUIT,
                builder.systemPrompt(Optional.empty(), Optional.empty()));
    }

    @Test
    void showsCircuitAsJsonAndSaysItIsUnchecked() throws Exception {
        Circuit halfAdder = CircuitTestFactory.halfAdder();

        String prompt = builder.systemPrompt(Optional.of(halfAdder), Optional.empty());

        assertTrue(prompt.startsWith(TutorPromptBuilder.RULES));
        assertTrue(prompt.contains(codec.toJson(describer.describe(halfAdder))));
        assertTrue(prompt.endsWith(TutorPromptBuilder.NOT_CHECKED));
    }

    @Test
    void listsFailingRows() throws Exception {
        Circuit halfAdder = CircuitTestFactory.halfAdder();
        CheckResult check = check(halfAdder, """
                inputs: A, B
                outputs: Sum, Cout
                0 0 | 0 0
                0 1 | 1 0
                1 0 | 1 0
                1 1 | 1 1
                """);

        String prompt = builder.systemPrompt(Optional.of(halfAdder), Optional.of(check));

        assertTrue(prompt.endsWith("""
                The circuit fails 1 of 4 rows of the expected truth table. Failing outputs: Sum.
                Failing rows:
                - A=1 B=1 -> Sum expected 1, got 0"""), prompt);
    }

    @Test
    void showsAtMostSixFailingRows() throws Exception {
        Circuit fullAdder = CircuitTestFactory.fullAdder();
        CheckResult check = check(fullAdder, """
                inputs: A, B, Cin
                outputs: Sum, Cout
                0 0 0 | 1 1
                0 0 1 | 0 1
                0 1 0 | 0 1
                0 1 1 | 1 0
                1 0 0 | 0 1
                1 0 1 | 1 0
                1 1 0 | 1 0
                1 1 1 | 0 0
                """);

        String prompt = builder.systemPrompt(Optional.of(fullAdder), Optional.of(check));

        assertTrue(prompt.contains("The circuit fails 8 of 8 rows"), prompt);
        assertEquals(TutorPromptBuilder.MAX_FAILING_ROWS, prompt.lines().filter(line -> line.startsWith("- A=")).count());
    }

    @Test
    void passingCircuitGetsCongratulated() throws Exception {
        Circuit halfAdder = CircuitTestFactory.halfAdder();
        CheckResult check = check(halfAdder, """
                inputs: A, B
                outputs: Sum, Cout
                0 0 | 0 0
                0 1 | 1 0
                1 0 | 1 0
                1 1 | 0 1
                """);

        String prompt = builder.systemPrompt(Optional.of(halfAdder), Optional.of(check));

        assertTrue(prompt.contains("passes all 4 rows"), prompt);
    }
}
