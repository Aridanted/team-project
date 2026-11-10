package entity.simulation;

/**
 * A three-valued logic signal. {@link #UNKNOWN} stands for a floating or undetermined value in a
 * simulation, and for "don't care" in an expected truth table.
 */
public enum Logic {
    ZERO('0'), ONE('1'), UNKNOWN('X');

    private final char symbol;

    Logic(char symbol) {
        this.symbol = symbol;
    }

    public static Logic of(boolean value) {
        return value ? ONE : ZERO;
    }

    /** @return the inverse; {@link #UNKNOWN} stays unknown */
    public Logic not() {
        return switch (this) {
            case ZERO -> ONE;
            case ONE -> ZERO;
            case UNKNOWN -> UNKNOWN;
        };
    }

    public boolean isKnown() {
        return this != UNKNOWN;
    }

    /** @return '0', '1' or 'X' */
    public char symbol() {
        return symbol;
    }
}
