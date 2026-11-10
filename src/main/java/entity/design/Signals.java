package entity.design;

import java.util.regex.Pattern;

/**
 * Rules for the signal names and signal references used in a {@link CircuitDesign}.
 *
 * <p>A <em>name</em> is an input label, an output label or a gate id: a letter followed by
 * letters, digits or underscores, at most {@value #MAX_NAME_LENGTH} characters (the same rule as
 * truth table labels). A <em>reference</em> is what a gate input or a connection points at: a
 * name, or the constant {@code 0} or {@code 1}; gate inputs may prefix it with {@code !} to
 * negate it.
 */
public final class Signals {

    public static final int MAX_NAME_LENGTH = 32;

    public static final String ZERO = "0";
    public static final String ONE = "1";

    private static final String NEGATION = "!";
    private static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");

    private Signals() {
    }

    /** @return whether {@code name} is a valid input label, output label or gate id */
    public static boolean isValidName(String name) {
        return name != null && name.length() <= MAX_NAME_LENGTH && NAME.matcher(name).matches();
    }

    /** @return whether {@code signal} is the constant {@code 0} or {@code 1} */
    public static boolean isConstant(String signal) {
        return ZERO.equals(signal) || ONE.equals(signal);
    }

    /** @return whether a gate input reference is negated, as in {@code !B} */
    public static boolean isNegated(String reference) {
        return reference.startsWith(NEGATION);
    }

    /** @return the referenced signal without any negation, so {@code !B} gives {@code B} */
    public static String signalOf(String reference) {
        return isNegated(reference) ? reference.substring(NEGATION.length()) : reference;
    }

    /** @return {@code signal} as a gate input reference, negated if asked */
    public static String reference(String signal, boolean negated) {
        return negated ? NEGATION + signal : signal;
    }
}
