package br.com.sisaws.material;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class MaterialRetriever {

    private static final Set<String> STOP_WORDS = Set.of(
            "para", "com", "uma", "uns", "das", "dos", "que", "por", "como", "qual",
            "quais", "sobre", "este", "esta", "isso", "ser", "tem", "sao", "mais", "aws",
            "segundo", "material", "entre"
    );

    private static final double SEMANTIC_WEIGHT = 0.70;
    private static final double LEXICAL_WEIGHT = 0.30;

    private final MaterialEmbeddingProvider embeddingProvider;

    public MaterialRetriever(MaterialEmbeddingProvider embeddingProvider) {
        this.embeddingProvider = embeddingProvider;
    }

    public RetrievalResult retrieve(String question, List<MaterialChunk> chunks, int limit) {
        Set<String> terms = tokenize(question);

        Map<MaterialChunk, Integer> lexicalScores = new LinkedHashMap<>();
        int maxLexical = 0;
        for (MaterialChunk chunk : chunks) {
            int score = lexicalScore(chunk.getContent(), terms);
            lexicalScores.put(chunk, score);
            maxLexical = Math.max(maxLexical, score);
        }

        Optional<MaterialEmbeddingProvider.EmbeddingVector> queryEmbedding =
                embeddingProvider.embed(question);

        boolean semanticAvailable = queryEmbedding.isPresent()
                && chunks.stream().anyMatch(chunk -> hasCompatibleEmbedding(
                        chunk,
                        queryEmbedding.get().modelId()
                ));

        final int lexicalMax = maxLexical;
        List<ScoredChunk> scored = chunks.stream()
                .map(chunk -> {
                    int lexical = lexicalScores.getOrDefault(chunk, 0);

                    if (!semanticAvailable) {
                        return new ScoredChunk(chunk, lexical, 0.0, lexical);
                    }

                    double lexicalNormalized = lexicalMax == 0
                            ? 0.0
                            : lexical / (double) lexicalMax;

                    double semantic = semanticSimilarity(queryEmbedding.get(), chunk);
                    double hybrid = semantic * SEMANTIC_WEIGHT
                            + lexicalNormalized * LEXICAL_WEIGHT;

                    return new ScoredChunk(chunk, lexical, semantic, hybrid);
                })
                .sorted(Comparator
                        .comparingDouble(ScoredChunk::combinedScore)
                        .reversed()
                        .thenComparingInt(item -> item.chunk().getChunkIndex()))
                .toList();

        if (semanticAvailable) {
            return new RetrievalResult(
                    scored.stream().limit(limit).toList(),
                    "SEMANTIC_HYBRID"
            );
        }

        List<ScoredChunk> positive = scored.stream()
                .filter(item -> item.lexicalScore() > 0)
                .limit(limit)
                .toList();

        return new RetrievalResult(
                positive.isEmpty() ? scored.stream().limit(limit).toList() : positive,
                "LEXICAL"
        );
    }

    private boolean hasCompatibleEmbedding(MaterialChunk chunk, String modelId) {
        return modelId.equals(chunk.getEmbeddingModel())
                && chunk.getEmbeddingVector().isPresent();
    }

    private double semanticSimilarity(
            MaterialEmbeddingProvider.EmbeddingVector queryEmbedding,
            MaterialChunk chunk) {

        if (!queryEmbedding.modelId().equals(chunk.getEmbeddingModel())) {
            return 0.0;
        }

        Optional<float[]> chunkVector = chunk.getEmbeddingVector();
        if (chunkVector.isEmpty()) {
            return 0.0;
        }

        double cosine = cosine(queryEmbedding.values(), chunkVector.get());
        return Math.max(0.0, Math.min(1.0, cosine));
    }

    private double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) {
            return 0.0;
        }

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private int lexicalScore(String content, Set<String> terms) {
        String normalized = normalize(content);
        int score = 0;

        for (String term : terms) {
            int index = 0;
            while ((index = normalized.indexOf(term, index)) >= 0) {
                score++;
                index += term.length();
            }
        }

        return Math.max(0, score - MaterialTextCleaner.rankingPenalty(content));
    }

    private Set<String> tokenize(String value) {
        return Arrays.stream(normalize(value).split("[^a-z0-9]+"))
                .filter(token -> token.length() >= 3)
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    public record ScoredChunk(
            MaterialChunk chunk,
            int lexicalScore,
            double semanticScore,
            double combinedScore
    ) {}

    public record RetrievalResult(
            List<ScoredChunk> chunks,
            String mode
    ) {}
}
