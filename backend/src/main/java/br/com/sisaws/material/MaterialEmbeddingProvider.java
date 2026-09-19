package br.com.sisaws.material;

import java.util.Optional;

public interface MaterialEmbeddingProvider {

    Optional<EmbeddingVector> embed(String text);

    boolean isEnabled();

    record EmbeddingVector(String modelId, float[] values) {}
}
