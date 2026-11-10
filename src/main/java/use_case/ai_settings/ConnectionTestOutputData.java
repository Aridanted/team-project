package use_case.ai_settings;

/**
 * @param providerName for example "Gemini"
 * @param problem      why the test failed, or {@code null} if it connected
 */
public record ConnectionTestOutputData(String providerName, ConnectionProblem problem) {

    public static ConnectionTestOutputData connected(String providerName) {
        return new ConnectionTestOutputData(providerName, null);
    }

    public boolean isConnected() {
        return problem == null;
    }
}
