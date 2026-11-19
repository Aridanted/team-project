package use_case;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TutorSafetyFilterTest {

    private final TutorSafetyFilter filter = new TutorSafetyFilter();

    @Test
    void removesCodeBlocksHeadingsAndCollapsesWhitespace() {
        String raw = "# Heading One\n\nHere is a hint.\n\n```java\nint x = 1;\n```\n\n   Too    many   spaces.  ";
        String result = filter.filter(raw);

        assertFalse(result.contains("```"));
        assertFalse(result.contains("#"));
        assertFalse(result.contains("  "));
        assertTrue(result.contains("Here is a hint."));
    }

    @Test
    void keepsAtMostSixSentences() {
        String raw = "Sentence one. Sentence two. Sentence three. Sentence four. Sentence five. Sentence six. Sentence seven. Sentence eight.";
        String result = filter.filter(raw);

        assertTrue(result.contains("Sentence six."));
        assertFalse(result.contains("Sentence seven."));
        assertFalse(result.contains("Sentence eight."));
    }

    @Test
    void replacesFullBooleanExpressionWithNudge() {
        String raw = "The answer is Sum = A ^ B ^ Cin for this adder.";
        String result = filter.filter(raw);

        assertFalse(result.contains("A ^ B ^ Cin"));
        assertTrue(result.toLowerCase().contains("step by step") || result.toLowerCase().contains("output"));
    }

    @Test
    void emptyOrBlankReplyCountsAsTutorUnavailable() {
        assertTrue(filter.filter("").toLowerCase().contains("unavailable"));
        assertTrue(filter.filter("   \n\t  ").toLowerCase().contains("unavailable"));
        assertTrue(filter.filter(null).toLowerCase().contains("unavailable"));
    }
}