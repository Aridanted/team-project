package entity.circuit;

/** The logic function a {@link Gate} computes over its (possibly negated) inputs. */
public enum GateType {
    AND(false),
    OR(false),
    NAND(false),
    NOR(false),
    /** One when an odd number of inputs are one (parity). */
    XOR(false),
    /** Inverse of {@link #XOR}. */
    XNOR(false),
    /** One when exactly one input is one. Equal to {@link #XOR} for two inputs. */
    XOR_ONE(false),
    /** Inverse of {@link #XOR_ONE}. */
    XNOR_ONE(false),
    NOT(true),
    BUFFER(true);

    private final boolean unary;

    GateType(boolean unary) {
        this.unary = unary;
    }

    /** @return whether gates of this type take exactly one input */
    public boolean isUnary() {
        return unary;
    }
}
