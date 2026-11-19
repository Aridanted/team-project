package data_access.llm;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

/**
 * Decorator that starts at most one call per {@code minInterval} (2 seconds in the app), waiting
 * as needed. Keeps free-tier quotas safe when a student clicks quickly. Calls are serialised.
 */
public final class RateLimitedLlmClient implements LlmClient {

    /** Waits; injected so tests do not really sleep. */
    @FunctionalInterface
    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private final LlmClient delegate;
    private final long minIntervalMillis;
    private final Clock clock;
    private final Sleeper sleeper;
    private boolean called;
    private long lastStartMillis;

    public RateLimitedLlmClient(LlmClient delegate, Duration minInterval) {
        this(delegate, minInterval, Clock.systemUTC(), Thread::sleep);
    }

    public RateLimitedLlmClient(LlmClient delegate, Duration minInterval, Clock clock, Sleeper sleeper) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.minIntervalMillis = Objects.requireNonNull(minInterval, "minInterval").toMillis();
        this.clock = Objects.requireNonNull(clock, "clock");
        this.sleeper = Objects.requireNonNull(sleeper, "sleeper");
    }

    @Override
    public synchronized String complete(LlmRequest request) throws LlmException {
        if (called) {
            long wait = lastStartMillis + minIntervalMillis - clock.millis();
            if (wait > 0) {
                try {
                    sleeper.sleep(wait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new LlmException(LlmException.Kind.NETWORK, "the AI call was cancelled", e);
                }
            }
        }
        called = true;
        lastStartMillis = clock.millis();
        return delegate.complete(request);
    }
}
