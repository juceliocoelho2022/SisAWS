package br.com.sisaws.material;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ValidationException;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class BedrockMaterialEmbeddingProvider implements MaterialEmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(BedrockMaterialEmbeddingProvider.class);
    private static final long AUTHORIZATION_COOLDOWN_MILLIS = 10 * 60 * 1000L;
    private static final int MAX_INPUT_CHARS = 12000;

    private final boolean enabled;
    private final String modelId;
    private final int dimensions;
    private final BedrockRuntimeClient client;
    private final ObjectMapper objectMapper;
    private final AtomicLong blockedUntilEpochMillis = new AtomicLong(0);

    public BedrockMaterialEmbeddingProvider(
            @Value("${sisaws.security.ai.embeddings-enabled:false}") boolean enabled,
            @Value("${sisaws.security.ai.bedrock-region:us-east-1}") String region,
            @Value("${sisaws.security.ai.embedding-model-id:amazon.titan-embed-text-v2:0}") String modelId,
            @Value("${sisaws.security.ai.embedding-dimensions:256}") int dimensions,
            ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.modelId = modelId;
        this.dimensions = dimensions;
        this.objectMapper = objectMapper;
        this.client = BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .build();
    }

    @Override
    public Optional<EmbeddingVector> embed(String text) {
        if (!canInvoke() || text == null || text.isBlank()) {
            return Optional.empty();
        }

        String input = text.length() <= MAX_INPUT_CHARS
                ? text
                : text.substring(0, MAX_INPUT_CHARS);

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("inputText", input);
        requestBody.put("dimensions", dimensions);
        requestBody.put("normalize", true);

        try {
            var response = client.invokeModel(InvokeModelRequest.builder()
                    .modelId(modelId)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(objectMapper.writeValueAsString(requestBody)))
                    .build());

            JsonNode embeddingNode = objectMapper
                    .readTree(response.body().asUtf8String())
                    .path("embedding");

            if (!embeddingNode.isArray() || embeddingNode.isEmpty()) {
                log.warn("Bedrock embedding response did not contain a vector");
                return Optional.empty();
            }

            float[] vector = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                vector[i] = (float) embeddingNode.get(i).asDouble();
            }

            return Optional.of(new EmbeddingVector(modelId, vector));
        } catch (ValidationException exception) {
            markTemporarilyUnavailable(exception);
            return Optional.empty();
        } catch (Exception exception) {
            log.warn("Bedrock embedding generation failed; lexical retrieval remains active", exception);
            return Optional.empty();
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private boolean canInvoke() {
        return enabled && System.currentTimeMillis() >= blockedUntilEpochMillis.get();
    }

    private void markTemporarilyUnavailable(ValidationException exception) {
        blockedUntilEpochMillis.set(System.currentTimeMillis() + AUTHORIZATION_COOLDOWN_MILLIS);
        log.warn(
                "Bedrock embeddings unavailable; lexical retrieval active for 10 minutes. status={}, requestId={}, message={}",
                exception.statusCode(),
                exception.requestId(),
                exception.getMessage()
        );
    }
}
