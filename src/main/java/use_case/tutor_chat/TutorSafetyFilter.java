package use_case.tutor_chat;

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
    /** Symbols and words that make a piece of text a Boolean expression, including negation. */
    private static final String OPERATOR = "(?:[&|^~!+*\\u00B7\\u2295\\u00AC\\u2227\\u2228]"
            + "|\\b(?:AND|OR|XOR|XNOR|NAND|NOR|NOT)\\b"
            + "|[A-Za-z0-9)]\\.(?=[A-Za-z(!~]))";
    /** "Y = ..." where the right side contains an operator, including NOT, "!" and "~". */
    private static final Pattern BOOLEAN_ASSIGNMENT_PATTERN = Pattern.compile(
            "\\b[A-Za-z_][A-Za-z0-9_]*\\s*:?=\\s*(?=(?:[^.;\\n?]|\\.(?!\\s|$))*" + OPERATOR + ")"
                    + "(?:[^.;\\n?]|\\.(?!\\s|$))*",
            Pattern.CASE_INSENSITIVE
    );
    /** Expressions without an assignment: "!(A & B)", "NOT (A AND B)", "NOT A", "A AND B", "A ^ B". */
    private static final Pattern BARE_EXPRESSION_PATTERN = Pattern.compile(
            "[!~\\u00AC]\\s*\\([^)]*\\)"
                    + "|\\bNOT\\s*\\([^)]*\\)"
                    + "|\\bNOT\\s+[A-Z][A-Za-z0-9_]?\\b"
                    + "|[!~\\u00AC][A-Za-z][A-Za-z0-9_]?\\b"
                    + "|\\b[A-Z][A-Za-z0-9_]?\\s+(?:AND|OR|XOR|XNOR|NAND|NOR)\\s+(?:NOT\\s+)?[A-Z][A-Za-z0-9_]?\\b"
                    + "|\\b[A-Za-z][A-Za-z0-9_]?\\s*[&|^\\u00B7\\u2295]+\\s*[!~\\u00AC]?\\s*[A-Za-z(]"
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

        cleaned = BOOLEAN_ASSIGNMENT_PATTERN.matcher(cleaned).replaceAll(GENERIC_NUDGE);
        cleaned = BARE_EXPRESSION_PATTERN.matcher(cleaned).replaceAll(GENERIC_NUDGE);

        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        cleaned = keepFirstNudge(cleaned);
        if (cleaned.isEmpty()) {
            return UNAVAILABLE_MESSAGE;
        }

        final String limited = limitSentences(cleaned, MAX_SENTENCES);
        return limited.isEmpty() ? UNAVAILABLE_MESSAGE : limited;
    }

    /** Several redacted expressions would repeat the same nudge, so only the first one stays. */
    private static String keepFirstNudge(String text) {
        final int first = text.indexOf(GENERIC_NUDGE);
        if (first < 0) {
            return text;
        }
        final int end = first + GENERIC_NUDGE.length();
        final String tail = text.substring(end).replace(GENERIC_NUDGE, " ");
        return (text.substring(0, end) + tail).replaceAll("\\s+\\.(?=\\s|$)", " ").replaceAll("\\s+", " ").trim();
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
