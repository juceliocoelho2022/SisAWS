package br.com.sisaws.material;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MaterialRetrieverTest {

    @Test
    void shouldUseLexicalFallbackWhenEmbeddingsAreUnavailable() {
        MaterialRetriever retriever = new MaterialRetriever(new DisabledEmbeddingProvider());

        MaterialChunk versioning = new MaterialChunk(
                null, 0, "O versionamento mantém versões anteriores de um objeto."
        );
        MaterialChunk encryption = new MaterialChunk(
                null, 1, "A criptografia protege objetos armazenados."
        );

        MaterialRetriever.RetrievalResult result = retriever.retrieve(
                "Como funciona o versionamento?",
                List.of(encryption, versioning),
                2
        );

        assertEquals("LEXICAL", result.mode());
        assertSame(versioning, result.chunks().getFirst().chunk());
    }

    @Test
    void shouldPreferSemanticMatchWhenCompatibleEmbeddingsExist() {
        StaticEmbeddingProvider provider = new StaticEmbeddingProvider(
                new float[]{1.0f, 0.0f, 0.0f}
        );
        MaterialRetriever retriever = new MaterialRetriever(provider);

        MaterialChunk lexical = new MaterialChunk(
                null, 0, "Armazenamento seguro em bucket com acesso controlado."
        );
        lexical.setEmbedding(new MaterialEmbeddingProvider.EmbeddingVector(
                "test-model", new float[]{0.0f, 1.0f, 0.0f}
        ));

        MaterialChunk semantic = new MaterialChunk(
                null, 1, "Preservar estados anteriores ajuda a recuperar mudanças acidentais."
        );
        semantic.setEmbedding(new MaterialEmbeddingProvider.EmbeddingVector(
                "test-model", new float[]{0.98f, 0.02f, 0.0f}
        ));

        MaterialRetriever.RetrievalResult result = retriever.retrieve(
                "Como recuperar uma versão anterior do arquivo?",
                List.of(lexical, semantic),
                2
        );

        assertEquals("SEMANTIC_HYBRID", result.mode());
        assertSame(semantic, result.chunks().getFirst().chunk());
        assertTrue(result.chunks().getFirst().semanticScore() > 0.9);
    }

    private static final class DisabledEmbeddingProvider implements MaterialEmbeddingProvider {
        @Override
        public Optional<EmbeddingVector> embed(String text) {
            return Optional.empty();
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    }

    private static final class StaticEmbeddingProvider implements MaterialEmbeddingProvider {
        private final float[] query;

        private StaticEmbeddingProvider(float[] query) {
            this.query = query;
        }

        @Override
        public Optional<EmbeddingVector> embed(String text) {
            return Optional.of(new EmbeddingVector("test-model", query));
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }
}
