package use_case.ai_settings;

import entity.settings.AiProvider;

/** Sends a tiny request to one AI provider to check that a key works. */
public interface AiConnectionTester {

    /** @throws AiConnectionException if the provider did not give a usable reply */
    void test(AiProvider provider, String apiKey) throws AiConnectionException;
}
