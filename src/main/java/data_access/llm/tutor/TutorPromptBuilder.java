package data_access.llm.tutor;

import data_access.json.CircuitJsonCodec;
import entity.circuit.Circuit;
import entity.design.CircuitDescriber;
import entity.design.NotDescribableException;
import entity.simulation.Logic;
import entity.truthtable.CheckResult;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Writes the tutor's system prompt: how to behave, then the student's circuit (in the shared
 * circuit JSON format) and the rows it fails. Deterministic, so tests compare exact text.
 */
public final class TutorPromptBuilder {

    /** At most this many failing rows are shown to the AI. */
    public static final int MAX_FAILING_ROWS = 6;

    static final String RULES = """
            You are LogiCoach, a patient tutor for an introductory digital logic course. The student \
            builds combinational circuits in Logisim and checks them against a truth table.
            Rules:
            - Guide with hints and questions. Never give the corrected circuit, a full list of gates, \
            or a full Boolean expression for an output.
            - If the student asks for the answer, say you will help them find it and give a smaller hint.
            - Answer in plain text, at most 6 sentences, with no code blocks or headings.
            - Refer to signals and gates only by the names given below.
            - End with one guiding question.
            - Reply in the language the student writes in.""";

    static final String NO_CIRCUIT = "The student has no circuit open. Answer general digital logic questions.";
    static final String NOT_CHECKED = "The circuit has not been checked against a truth table yet.";

    private final CircuitDescriber describer;
    private final CircuitJsonCodec codec;

    public TutorPromptBuilder(CircuitDescriber describer, CircuitJsonCodec codec) {
        this.describer = Objects.requireNonNull(describer, "describer");
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    public String systemPrompt(Optional<Circuit> circuit, Optional<CheckResult> lastCheck) {
        StringBuilder prompt = new StringBuilder(RULES).append("\n\n");
        if (circuit.isEmpty()) {
            return prompt.append(NO_CIRCUIT).toString();
        }
        prompt.append(describe(circuit.get())).append("\n\n");
        prompt.append(lastCheck.map(TutorPromptBuilder::describe).orElse(NOT_CHECKED));
        return prompt.toString();
    }

    private String describe(Circuit circuit) {
        try {
            return "The student's circuit (JSON; gates are named g1, g2, ... from left to right):\n"
                    + codec.toJson(describer.describe(circuit));
        } catch (NotDescribableException e) {
            return "The student's circuit \"" + circuit.name() + "\" cannot be shown to you: " + e.getMessage();
        }
    }

    private static String describe(CheckResult check) {
        if (check.allPassed()) {
            return "The circuit passes all " + check.totalRows() + " rows of the expected truth table. "
                    + "Congratulate the student and offer a harder challenge if they want one.";
        }
        List<String> inputs = check.expected().inputLabels();
        List<String> outputs = check.expected().outputLabels();
        StringBuilder text = new StringBuilder("The circuit fails ")
                .append(check.totalRows() - check.passedRows()).append(" of ").append(check.totalRows())
                .append(" rows of the expected truth table. Failing outputs: ")
                .append(String.join(", ", check.failingOutputs())).append(".\nFailing rows:");
        for (CheckResult.RowResult row : check.failingRows().stream().limit(MAX_FAILING_ROWS).toList()) {
            text.append("\n- ").append(assignments(inputs, row.inputs())).append(" ->");
            for (String output : row.failingOutputs()) {
                int index = outputs.indexOf(output);
                text.append(' ').append(output).append(" expected ").append(row.expected().get(index).symbol())
                        .append(", got ").append(row.actual().get(index).symbol()).append(';');
            }
            text.setLength(text.length() - 1);
        }
        return text.toString();
    }

    private static String assignments(List<String> labels, List<Logic> values) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < labels.size(); i++) {
            text.append(i == 0 ? "" : " ").append(labels.get(i)).append('=').append(values.get(i).symbol());
        }
        return text.toString();
    }
}
