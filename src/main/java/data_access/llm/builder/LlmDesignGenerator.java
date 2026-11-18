package data_access.llm.builder;

import data_access.json.CircuitJsonCodec;
import data_access.json.DesignFormatException;
import data_access.llm.LlmClient;
import data_access.llm.LlmException;
import data_access.llm.LlmMessage;
import data_access.llm.LlmRequest;
import entity.design.CircuitDesign;
import org.json.JSONException;
import org.json.JSONObject;
import use_case.build_circuit.CircuitDesignGenerator;
import use_case.build_circuit.DesignGenerationException;
import use_case.build_circuit.GeneratedDesign;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Generates circuit designs using an AI client and the shared circuit JSON format. */
public final class LlmDesignGenerator implements CircuitDesignGenerator {
    private static final double TEMPERATURE = 0.2;
    private static final int MAX_OUTPUT_TOKENS = 4096;

    private final LlmClient client;
    private final CircuitDesignPromptBuilder prompts;
    private final CircuitJsonCodec codec;

    /**
     * Creates a generator with its client, prompt builder, and circuit codec.
     *
     * @param client the AI client
     * @param prompts the circuit generation instructions
     * @param codec the shared circuit JSON codec
     */
    public LlmDesignGenerator(LlmClient client, CircuitDesignPromptBuilder prompts,
                              CircuitJsonCodec codec) {
        this.client = Objects.requireNonNull(client, "client");
        this.prompts = Objects.requireNonNull(prompts, "prompts");
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    @Override
    public GeneratedDesign generate(String description) throws DesignGenerationException {
        LlmRequest request = new LlmRequest(prompts.systemPrompt(),
                List.of(LlmMessage.user(description)), TEMPERATURE, MAX_OUTPUT_TOKENS, true);

        try {
            String reply = client.complete(request);
            CircuitDesign design = codec.fromJson(reply);
            JSONObject root = readObject(reply);
            Map<String, String> expressions = readExpressions(root);
            String explanation = readExplanation(root);
            return new GeneratedDesign(design, expressions, explanation);
        } catch (LlmException error) {
            throw new DesignGenerationException("AI generation failed. " + error.getMessage());
        } catch (DesignFormatException | JSONException error) {
            throw new DesignGenerationException("Invalid generated design. " + error.getMessage());
        }
    }

    private JSONObject readObject(String reply) {
        // The codec has already validated the object between these braces.
        int start = reply.indexOf('{');
        int end = reply.lastIndexOf('}');
        return new JSONObject(reply.substring(start, end + 1));
    }

    private Map<String, String> readExpressions(JSONObject root) throws DesignFormatException {
        if (!root.has("expressions")) {
            return Map.of();
        }

        JSONObject expressions = root.optJSONObject("expressions");
        if (expressions == null) {
            throw new DesignFormatException("The expressions field must be an object.");
        }

        Map<String, String> result = new HashMap<>();
        for (String output : expressions.keySet()) {
            Object value = expressions.get(output);
            if (!(value instanceof String expression)) {
                throw new DesignFormatException("Expression for output " + output + " must be a string.");
            }
            result.put(output, expression);
        }
        return Map.copyOf(result);
    }

    private String readExplanation(JSONObject root) throws DesignFormatException {
        Object value = root.opt("explanation");
        if (!(value instanceof String explanation) || explanation.isBlank()) {
            throw new DesignFormatException("The explanation field must be a nonblank string.");
        }
        return explanation.strip();
    }
}
