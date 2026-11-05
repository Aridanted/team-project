package data_access.llm;

import java.io.IOException;
import java.util.Map;

/** Records the last request and answers with a canned response or an I/O failure. */
final class FakeHttpTransport implements HttpTransport {

    private final HttpReply reply;
    private final IOException failure;
    String url;
    Map<String, String> headers;
    String body;
    int calls;

    private FakeHttpTransport(HttpReply reply, IOException failure) {
        this.reply = reply;
        this.failure = failure;
    }

    static FakeHttpTransport answering(int status, String body) {
        return new FakeHttpTransport(new HttpReply(status, body), null);
    }

    static FakeHttpTransport failing(IOException failure) {
        return new FakeHttpTransport(null, failure);
    }

    @Override
    public HttpReply postJson(String url, Map<String, String> headers, String body) throws IOException {
        this.url = url;
        this.headers = headers;
        this.body = body;
        calls++;
        if (failure != null) {
            throw failure;
        }
        return reply;
    }
}
