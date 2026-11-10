package entity.circuit;

import java.util.Comparator;

/**
 * Natural ordering for pin labels: case-insensitive, with digit runs compared by value, so
 * {@code IN2} comes before {@code IN10}. Defines the column order of truth tables.
 */
public final class LabelOrder implements Comparator<String> {

    public static final LabelOrder NATURAL = new LabelOrder();

    private LabelOrder() {
    }

    @Override
    public int compare(String a, String b) {
        int i = 0;
        int j = 0;
        while (i < a.length() && j < b.length()) {
            char ca = a.charAt(i);
            char cb = b.charAt(j);
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                int endA = endOfDigits(a, i);
                int endB = endOfDigits(b, j);
                int byValue = compareDigitRuns(a.substring(i, endA), b.substring(j, endB));
                if (byValue != 0) {
                    return byValue;
                }
                i = endA;
                j = endB;
            } else {
                int byChar = Character.compare(Character.toLowerCase(ca), Character.toLowerCase(cb));
                if (byChar != 0) {
                    return byChar;
                }
                i++;
                j++;
            }
        }
        int byLength = Integer.compare(a.length() - i, b.length() - j);
        return byLength != 0 ? byLength : a.compareTo(b);
    }

    private static int endOfDigits(String s, int start) {
        int end = start;
        while (end < s.length() && Character.isDigit(s.charAt(end))) {
            end++;
        }
        return end;
    }

    private static int compareDigitRuns(String a, String b) {
        String trimmedA = a.replaceFirst("^0+(?=.)", "");
        String trimmedB = b.replaceFirst("^0+(?=.)", "");
        int byLength = Integer.compare(trimmedA.length(), trimmedB.length());
        return byLength != 0 ? byLength : trimmedA.compareTo(trimmedB);
    }
}
