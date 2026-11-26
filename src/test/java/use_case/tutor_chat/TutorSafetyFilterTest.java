package use_case.tutor_chat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TutorSafetyFilterTest {

    private final TutorSafetyFilter filter = new TutorSafetyFilter();

    @Test
    void removesCodeBlocksHeadingsAndCollapsesWhitespace() {
        final String raw = "# Heading One\n\nHere is a hint.\n\n"
                + "```java\nint x = 1;\n```\n\n   Too    many   spaces.  ";
        final String result = filter.filter(raw);

        assertFalse(result.contains("```"));
        assertFalse(result.contains("#"));
        assertFalse(result.contains("  "));
        assertTrue(result.contains("Here is a hint."));
    }

    @Test
    void keepsAtMostSixSentences() {
        final String raw = "Sentence one. Sentence two. Sentence three. "
                + "Sentence four. Sentence five. Sentence six. Sentence seven. Sentence eight.";
        final String result = filter.filter(raw);

        assertTrue(result.contains("Sentence six."));
        assertFalse(result.contains("Sentence seven."));
        assertFalse(result.contains("Sentence eight."));
    }

    @Test
    void replacesFullBooleanExpressionWithNudge() {
        final String raw = "The answer is Sum = A ^ B ^ Cin for this adder.";
        final String result = filter.filter(raw);

        assertFalse(result.contains("A ^ B ^ Cin"));
        assertTrue(result.toLowerCase().contains("step by step")
                || result.toLowerCase().contains("output"));
    }

    @Test
    void emptyOrBlankReplyCountsAsTutorUnavailable() {
        assertTrue(filter.filter("").toLowerCase().contains("unavailable"));
        assertTrue(filter.filter("   \n\t  ").toLowerCase().contains("unavailable"));
        assertTrue(filter.filter(null).toLowerCase().contains("unavailable"));
    }

    @Test
    void removesNegatedAnswersInEveryNotilation() {
        final String[] leaks = {
            "The answer is Y = !(A & B) for this gate.",
            "Use Y = NOT (A AND B) here.",
            "Try Y = NOT A to fix it.",
            "You want Y = ~(A | B).",
            "It should be Y = !A + B.",
            "Then Y = A NAND B works.",
            "Write `!(A & B)` on the output.",
            "Put a NOT(A AND B) gate there.",
            "The output is NOT A for that row.",
            "Just use A AND B on the output.",
        };
        for (String leak : leaks) {
            final String result = filter.filter(leak);
            assertFalse(result.contains("!(") || result.contains("~(") || result.contains("NOT")
                    || result.contains("NAND") || result.contains("A & B") || result.contains("A AND B")
                    || result.contains("!A"), "leaked: " + leak + " -> " + result);
        }
    }

    @Test
    void keepsPlainAdviceThatMentionsNotAsAnEnglishWord() {
        final String raw = "That is not quite right. Think about what an inverter does to the input.";

        assertTrue(filter.filter(raw).contains("not quite right"));
    }

    @Test
    void keepsOfflineTutorHintsIntact() {
        final String hint = "Offline Tutor (Rule-based): Hint 2: 1 of 2 rows fail, on output(s) Y. "
                + "Trace row 2 (A is 1, B is 0) through the gates that drive Y, "
                + "and compare each gate's output with what you expect.";

        assertTrue(filter.filter(hint).contains("Trace row 2 (A is 1, B is 0)"));
    }

    @Test
    void repeatedNudgesAreCollapsed() {
        final String result = filter.filter("Y = !A. Z = !B.");

        assertTrue(result.indexOf("step by step") == result.lastIndexOf("step by step"));
    }
}
