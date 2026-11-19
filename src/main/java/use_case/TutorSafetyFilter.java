package use_case;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filters tutor replies so they stay concise, remove code/headings, and never give full answers.
 */
public class TutorSafetyFilter {

    private static final String GENERIC_NUDGE =
            "Let's check step by step: which inputs affect that output, and how does each gate behave?";
    private static final String UNAVAILABLE_MESSAGE =
            "Tutor unavailable right now. Try checking your circuit or asking a specific hint question.";
    private static final int MAX_SENTENCES = 6;
    private static final Pattern BOOLEAN_ASSIGNMENT_PATTERN = Pattern.compile(
            "\\b[A-Za-z_][A-Za-z0-9_]*\\s*=\\s*[^.;!?\\n]*[&|^~+·⊕][^.;!?\\n]*",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[^.!?]+[.!?]?");

    /**
     * Sanitizes a raw tutor reply before displaying it to the student.
     *
     * @param rawReply the raw response text
     * @return a safe, concise response string
     */
    public String filter(String rawReply) {
        if (rawReply == null || rawReply.trim().isEmpty()) {
            return UNAVAILABLE_MESSAGE;
        }

        String cleaned = rawReply.replaceAll("(?s)```.*?```", " ");
        cleaned = cleaned.replaceAll("`([^`]*)`", "$1");
        cleaned = cleaned.replaceAll("(?m)^\\s*#{1,6}\\s*.*$", " ");

        final Matcher expressionMatcher = BOOLEAN_ASSIGNMENT_PATTERN.matcher(cleaned);
        cleaned = expressionMatcher.replaceAll(GENERIC_NUDGE);

        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty()) {
            return UNAVAILABLE_MESSAGE;
        }

        final String limited = limitSentences(cleaned, MAX_SENTENCES);
        return limited.isEmpty() ? UNAVAILABLE_MESSAGE : limited;
    }

    /**
     * Limits the given text to at most {@code maxSentences} sentences.
     *
     * @param text the cleaned text
     * @param maxSentences maximum number of sentences to keep
     * @return the sentence-limited text
     */
    private String limitSentences(String text, int maxSentences) {
        final List<String> sentences = new ArrayList<>();
        final Matcher matcher = SENTENCE_PATTERN.matcher(text);

        while (matcher.find() && sentences.size() < maxSentences) {
            final String sentence = matcher.group().trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
        }

        return String.join(" ", sentences).trim();
    }
}
