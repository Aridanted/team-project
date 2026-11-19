package data_access.llm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * An {@link LlmClient} for tests: returns scripted replies or throws scripted errors, in order,
 * and records every request it received. Never touches the network.
 *
 * <pre>
 * FakeLlmClient ai = FakeLlmClient.replying("{\"name\": ...}")
 *         .thenFail(LlmException.Kind.RATE_LIMIT)
 *         .thenReply("second answer");
 * ...
 * assertEquals(3, ai.requests().size());
 * assertTrue(ai.lastRequest().expectJson());
 * </pre>
 *
 * When the script runs out, the last entry is repeated if it was added with {@code always...};
 * otherwise the call fails the test with an {@link IllegalStateException}.
 */
public final class FakeLlmClient implements LlmClient {

    private final Deque<Step> script = new ArrayDeque<>();
    private final List<LlmRequest> requests = new ArrayList<>();
    private Step forever;

    private interface Step {
        String run() throws LlmException;
    }

    public static FakeLlmClient replying(String... replies) {
        FakeLlmClient client = new FakeLlmClient();
        for (String reply : replies) {
            client.thenReply(reply);
        }
        return client;
    }

    public static FakeLlmClient alwaysReplying(String reply) {
        FakeLlmClient client = new FakeLlmClient();
        client.forever = () -> reply;
        return client;
    }

    public static FakeLlmClient alwaysFailing(LlmException.Kind kind) {
        FakeLlmClient client = new FakeLlmClient();
        client.forever = () -> {
            throw new LlmException(kind, "fake " + kind);
        };
        return client;
    }

    public FakeLlmClient thenReply(String reply) {
        script.add(() -> reply);
        return this;
    }

    public FakeLlmClient thenFail(LlmException.Kind kind) {
        return thenFail(new LlmException(kind, "fake " + kind));
    }

    public FakeLlmClient thenFail(LlmException error) {
        script.add(() -> {
            throw error;
        });
        return this;
    }

    @Override
    public synchronized String complete(LlmRequest request) throws LlmException {
        requests.add(request);
        Step next = script.isEmpty() ? forever : script.poll();
        if (next == null) {
            throw new IllegalStateException("FakeLlmClient got request " + requests.size() + " but has no reply left");
        }
        return next.run();
    }

    public synchronized List<LlmRequest> requests() {
        return List.copyOf(requests);
    }

    public synchronized LlmRequest lastRequest() {
        if (requests.isEmpty()) {
            throw new IllegalStateException("no request was made");
        }
        return requests.get(requests.size() - 1);
    }
}
