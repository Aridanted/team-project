package data_access.llm;

import java.io.IOException;
import java.util.Map;

/**
 * The one HTTP call the AI clients need. Injected so tests can hand clients canned responses
 * without touching the network.
 */
public interface HttpTransport {

    /**
     * Sends a JSON POST request.
     *
     * @param headers extra headers; {@code Content-Type: application/json} is always sent
     * @return the status and body of the response, whatever the status
     * @throws IOException if no response arrived (connection failed, timed out, or interrupted)
     */
    HttpReply postJson(String url, Map<String, String> headers, String body) throws IOException;

    /** An HTTP response. */
    record HttpReply(int status, String body) {

        public HttpReply {
            body = body == null ? "" : body;
        }

        public boolean isSuccess() {
            return status >= 200 && status < 300;
        }
    }
}
