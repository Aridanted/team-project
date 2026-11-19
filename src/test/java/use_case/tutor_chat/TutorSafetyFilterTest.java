package use_case.tutor_chat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import use_case.TutorSafetyFilter;

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
}
