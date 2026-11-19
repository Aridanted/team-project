package data_access.llm;

import org.json.JSONException;
import org.json.JSONObject;

/** Builds {@link LlmException}s from provider responses without leaking keys. */
final class ProviderErrors {

    private static final int MAX_DETAIL_LENGTH = 200;

    private ProviderErrors() {
    }

    /**
     * @return "{provider}: HTTP {status}: {error.message}" with the key masked and the detail
     *         shortened; the detail is left out if the body is not the usual error JSON
     */
    static String describe(String provider, int status, String body, String apiKey) {
        String detail = errorMessage(body);
        String text = provider + ": HTTP " + status + (detail.isEmpty() ? "" : ": " + detail);
        return mask(text, apiKey);
    }

    static String mask(String text, String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return text;
        }
        return text.replace(apiKey, "***");
    }

    private static String errorMessage(String body) {
        try {
            JSONObject error = new JSONObject(body).optJSONObject("error");
            String message = error == null ? "" : error.optString("message", "").strip();
            return message.length() > MAX_DETAIL_LENGTH ? message.substring(0, MAX_DETAIL_LENGTH) + "..." : message;
        } catch (JSONException e) {
            return "";
        }
    }
}
