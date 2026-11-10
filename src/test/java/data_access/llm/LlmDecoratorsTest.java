package data_access.llm;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Fallback, rate limiting and the no-key null object. */
class LlmDecoratorsTest {

    private static final LlmRequest REQUEST = LlmRequest.single("system", "hello");

    @Test
    void fallbackUsesPrimaryWhenItWorks() throws LlmException {
        FakeLlmClient secondary = FakeLlmClient.replying();

        assertEquals("first", new FallbackLlmClient(FakeLlmClient.replying("first"), secondary).complete(REQUEST));
        assertTrue(secondary.requests().isEmpty());
    }

    @Test
    void fallbackTriesSecondaryWithTheSameRequest() throws LlmException {
        FakeLlmClient secondary = FakeLlmClient.replying("second");

        String reply = new FallbackLlmClient(FakeLlmClient.alwaysFailing(LlmException.Kind.RATE_LIMIT), secondary)
                .complete(REQUEST);

        assertEquals("second", reply);
        assertSame(REQUEST, secondary.lastRequest());
    }

    @Test
    void whenBothFailThePrimaryErrorWinsUnlessItOnlyLackedAKey() {
        LlmException both = assertThrows(LlmException.class, () -> new FallbackLlmClient(
                FakeLlmClient.alwaysFailing(LlmException.Kind.AUTH),
                FakeLlmClient.alwaysFailing(LlmException.Kind.NETWORK)).complete(REQUEST));
        LlmException noKeyFirst = assertThrows(LlmException.class, () -> new FallbackLlmClient(
                new UnavailableLlmClient(), FakeLlmClient.alwaysFailing(LlmException.Kind.NETWORK)).complete(REQUEST));

        assertEquals(LlmException.Kind.AUTH, both.kind());
        assertEquals(1, both.getSuppressed().length);
        assertEquals(LlmException.Kind.NETWORK, noKeyFirst.kind());
    }

    @Test
    void unavailableClientAlwaysSaysNoKey() {
        LlmException e = assertThrows(LlmException.class, () -> new UnavailableLlmClient().complete(REQUEST));

        assertEquals(LlmException.Kind.NO_KEY, e.kind());
        assertEquals(UnavailableLlmClient.MESSAGE, e.getMessage());
    }

    @Test
    void rateLimiterWaitsOnlyForTheRestOfTheInterval() throws LlmException {
        MutableClock clock = new MutableClock();
        List<Long> sleeps = new ArrayList<>();
        RateLimitedLlmClient client = new RateLimitedLlmClient(FakeLlmClient.alwaysReplying("ok"),
                Duration.ofSeconds(2), clock, millis -> {
                    sleeps.add(millis);
                    clock.advance(millis);
                });

        client.complete(REQUEST);
        clock.advance(500);
        client.complete(REQUEST);
        clock.advance(5000);
        client.complete(REQUEST);

        assertEquals(List.of(1500L), sleeps);
    }

    @Test
    void rateLimiterCountsFailedCallsToo() {
        MutableClock clock = new MutableClock();
        List<Long> sleeps = new ArrayList<>();
        RateLimitedLlmClient client = new RateLimitedLlmClient(FakeLlmClient.alwaysFailing(LlmException.Kind.NETWORK),
                Duration.ofSeconds(2), clock, millis -> {
                    sleeps.add(millis);
                    clock.advance(millis);
                });

        assertThrows(LlmException.class, () -> client.complete(REQUEST));
        assertThrows(LlmException.class, () -> client.complete(REQUEST));

        assertEquals(List.of(2000L), sleeps);
    }

    @Test
    void interruptedWaitBecomesNetworkError() throws LlmException {
        RateLimitedLlmClient client = new RateLimitedLlmClient(FakeLlmClient.alwaysReplying("ok"),
                Duration.ofSeconds(2), new MutableClock(), millis -> {
                    throw new InterruptedException();
                });
        client.complete(REQUEST);

        LlmException e = assertThrows(LlmException.class, () -> client.complete(REQUEST));

        assertEquals(LlmException.Kind.NETWORK, e.kind());
        assertTrue(Thread.interrupted());
    }

    @Test
    void requestValidation() {
        assertThrows(IllegalArgumentException.class, () -> new LlmRequest("", List.of(), 0.5, 10, false));
        assertThrows(IllegalArgumentException.class, () -> new LlmRequest("",
                List.of(LlmMessage.user("a"), LlmMessage.assistant("b")), 0.5, 10, false));
        assertThrows(IllegalArgumentException.class, () -> new LlmRequest("", List.of(LlmMessage.user("a")), 2.5, 10,
                false));
        assertThrows(IllegalArgumentException.class, () -> new LlmRequest("", List.of(LlmMessage.user("a")), 0.5, 0,
                false));
        assertThrows(IllegalArgumentException.class, () -> LlmMessage.user(" "));
        LlmRequest single = LlmRequest.single("s", "u");
        assertEquals(LlmRequest.DEFAULT_MAX_OUTPUT_TOKENS, single.maxOutputTokens());
        assertEquals(LlmRole.USER, single.messages().get(0).role());
    }

    private static final class MutableClock extends Clock {
        private long millis = 1_000_000;

        void advance(long by) {
            millis += by;
        }

        @Override
        public long millis() {
            return millis;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
