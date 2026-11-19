package data_access.llm.builder;

import data_access.json.CircuitJsonCodec;
import data_access.llm.FakeLlmClient;
import data_access.llm.LlmException;
import data_access.llm.LlmMessage;
import data_access.llm.LlmRequest;
import entity.circuit.GateType;
import org.junit.jupiter.api.Test;
import use_case.build_circuit.DesignGenerationException;
import use_case.build_circuit.GeneratedDesign;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmDesignGeneratorTest {
    private static final String VALID_REPLY = """
            {
              "name": "Inverter",
              "inputs": ["A"],
              "outputs": ["Y"],
              "gates": [
                {"id": "G1", "type": "NOT", "inputs": ["A"]}
              ],
              "connections": {"Y": "G1"},
              "explanation": "This circuit inverts A. Y is one when A is zero.",
              "expressions": {"Y": "!A"}
            }
            """;

    @Test
    void parsesTheDesignAndOutputExpressions() throws DesignGenerationException {
        FakeLlmClient client = FakeLlmClient.replying(VALID_REPLY);

        GeneratedDesign generated = generator(client).generate("Invert input A");

        assertEquals("Inverter", generated.design().name());
        assertEquals(List.of("A"), generated.design().inputs());
        assertEquals(List.of("Y"), generated.design().outputs());
        assertEquals(GateType.NOT, generated.design().gates().getFirst().type());
        assertEquals(Map.of("Y", "G1"), generated.design().connections());
        assertEquals(Map.of("Y", "!A"), generated.outputExpressions());
        assertEquals("This circuit inverts A. Y is one when A is zero.", generated.explanation());
    }

    @Test
    void sendsTheDescriptionAndRequestsJson() throws DesignGenerationException {
        FakeLlmClient client = FakeLlmClient.replying(VALID_REPLY);

        generator(client).generate("Invert input A");

        LlmRequest request = client.lastRequest();
        assertEquals(1, client.requests().size());
        assertEquals(List.of(LlmMessage.user("Invert input A")), request.messages());
        assertTrue(request.expectJson());
        assertTrue(request.systemPrompt().contains("2 to 4 sentences"));
        assertTrue(request.systemPrompt().contains("Return JSON only"));
    }

    @Test
    void acceptsADesignWithoutOptionalExpressions() throws DesignGenerationException {
        String reply = VALID_REPLY.replace(
                ",\n  \"expressions\": {\"Y\": \"!A\"}", "");
        FakeLlmClient client = FakeLlmClient.replying(reply);

        GeneratedDesign generated = generator(client).generate("Invert input A");

        assertEquals(Map.of(), generated.outputExpressions());
    }

    @Test
    void rejectsAReplyWithoutJson() {
        FakeLlmClient client = FakeLlmClient.replying("I could not produce a circuit.");

        DesignGenerationException error = assertThrows(DesignGenerationException.class,
                () -> generator(client).generate("Invert input A"));

        assertTrue(error.getMessage().contains("Invalid generated design"));
    }

    @Test
    void rejectsANonStringOutputExpression() {
        String reply = VALID_REPLY.replace("\"Y\": \"!A\"", "\"Y\": 42");
        FakeLlmClient client = FakeLlmClient.replying(reply);

        DesignGenerationException error = assertThrows(DesignGenerationException.class,
                () -> generator(client).generate("Invert input A"));

        assertTrue(error.getMessage().contains("must be a string"));
    }

    @Test
    void convertsClientFailuresIntoGenerationFailures() {
        FakeLlmClient client = FakeLlmClient.alwaysFailing(LlmException.Kind.NETWORK);

        DesignGenerationException error = assertThrows(DesignGenerationException.class,
                () -> generator(client).generate("Invert input A"));

        assertTrue(error.getMessage().contains("AI generation failed"));
        assertEquals(1, client.requests().size());
    }

    @Test
    void rejectsABlankExplanation() {
        String reply = VALID_REPLY.replace(
                "This circuit inverts A. Y is one when A is zero.", "");
        FakeLlmClient client = FakeLlmClient.replying(reply);

        DesignGenerationException error = assertThrows(DesignGenerationException.class,
                () -> generator(client).generate("Invert input A"));

        assertTrue(error.getMessage().contains("explanation"));
    }

    private static LlmDesignGenerator generator(FakeLlmClient client) {
        return new LlmDesignGenerator(client, new CircuitDesignPromptBuilder(), new CircuitJsonCodec());
    }
}
