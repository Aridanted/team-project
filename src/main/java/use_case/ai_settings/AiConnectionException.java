package use_case.ai_settings;

import java.util.Objects;

/** Thrown by an {@link AiConnectionTester} when the test request did not get a usable reply. */
public class AiConnectionException extends Exception {

    private static final long serialVersionUID = 1L;

    private final ConnectionProblem problem;

    /** @param message technical detail, never containing keys or prompts */
    public AiConnectionException(ConnectionProblem problem, String message) {
        super(message);
        this.problem = Objects.requireNonNull(problem, "problem");
    }

    public ConnectionProblem problem() {
        return problem;
    }
}
