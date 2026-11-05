package data_access.llm;

import java.util.Objects;

/**
 * Decorator: asks {@code primary}, and if that fails for any reason asks {@code secondary}.
 * Chains of more providers are built by nesting.
 *
 * <p>If both fail, the primary's error is thrown (with the secondary's attached as suppressed),
 * unless the primary only lacked a key, in which case the secondary's error says more.
 */
public final class FallbackLlmClient implements LlmClient {

    private final LlmClient primary;
    private final LlmClient secondary;

    public FallbackLlmClient(LlmClient primary, LlmClient secondary) {
        this.primary = Objects.requireNonNull(primary, "primary");
        this.secondary = Objects.requireNonNull(secondary, "secondary");
    }

    @Override
    public String complete(LlmRequest request) throws LlmException {
        try {
            return primary.complete(request);
        } catch (LlmException primaryError) {
            try {
                return secondary.complete(request);
            } catch (LlmException secondaryError) {
                if (primaryError.kind() == LlmException.Kind.NO_KEY) {
                    secondaryError.addSuppressed(primaryError);
                    throw secondaryError;
                }
                primaryError.addSuppressed(secondaryError);
                throw primaryError;
            }
        }
    }
}
