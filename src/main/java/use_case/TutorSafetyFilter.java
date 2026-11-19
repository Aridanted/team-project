package use_case;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TutorSafetyFilter {

    private static final String GENERIC_NUDGE =
            "Let's think step by step: which input combinations change the target output, and how does each gate behave?";
    private static final String UNAVAILABLE_MESSAGE =
            "Tutor unavailable right now. Please try again or check your circuit step by step.";

    private static final Pattern BOOLEAN_ASSIGNMENT_PATTERN = Pattern.compile(
            "\\b[A-Za-z_][A-Za-z0-9_]*\\s*=\\s*[^.;!?\\n]*[&|^~+·⊕andorxornot][^.;!?\\n]*",
            Pattern.CASE_INSENSITIVE
    );

    public String filter(String rawReply) {
        if (rawReply == null || rawReply.trim().isEmpty()) {
            return UNAVAILABLE_MESSAGE;
        }

        String cleaned = rawReply;

        cleaned = cleaned.replaceAll("(?s)```.*?```", " ");
        cleaned = cleaned.replaceAll("`([^`]*)`", "$1");

        cleaned = cleaned.replaceAll("(?m)^\\s*#{1,6}\\s*.*$", " ");

        Matcher matcher = BOOLEAN_ASSIGNMENT_PATTERN.matcher(cleaned);
        if (matcher.find()) {
            cleaned = matcher.replaceAll(GENERIC_NUDGE);
        }

        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        if (cleaned.isEmpty()) {
            return UNAVAILABLE_MESSAGE;
        }

        cleaned = limitSentences(cleaned, 6);

        return cleaned.isEmpty() ? UNAVAILABLE_MESSAGE : cleaned;
    }

    private String limitSentences(String text, int maxSentences) {
        List<String> sentences = new ArrayList<>();
        Matcher sentenceMatcher = Pattern.compile("[^.!?]+[.!?]?").matcher(text);

        while (sentenceMatcher.find() && sentences.size() < maxSentences) {
            String s = sentenceMatcher.group().trim();
            if (!s.isEmpty()) {
                sentences.add(s);
            }
        }

        return String.join(" ", sentences).trim();
    }
}