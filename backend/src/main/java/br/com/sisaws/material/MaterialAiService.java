package br.com.sisaws.material;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MaterialAiService {

    private static final Set<String> STOP_WORDS = Set.of(
            "para", "com", "uma", "uns", "das", "dos", "que", "por", "como", "qual",
            "quais", "sobre", "este", "esta", "isso", "ser", "tem", "são", "mais", "aws"
    );

    private final StudyMaterialRepository materialRepository;
    private final MaterialChunkRepository chunkRepository;
    private final MaterialAiProfileRepository profileRepository;
    private final MaterialAiFlashcardRepository flashcardRepository;
    private final MaterialStorageService storage;
    private final MaterialExtractionService extraction;
    private final BedrockStudyGenerator generator;
    private final LocalStudyGenerator localGenerator;

    public MaterialAiService(
            StudyMaterialRepository materialRepository,
            MaterialChunkRepository chunkRepository,
            MaterialAiProfileRepository profileRepository,
            MaterialAiFlashcardRepository flashcardRepository,
            MaterialStorageService storage,
            MaterialExtractionService extraction,
            BedrockStudyGenerator generator,
            LocalStudyGenerator localGenerator) {
        this.materialRepository = materialRepository;
        this.chunkRepository = chunkRepository;
        this.profileRepository = profileRepository;
        this.flashcardRepository = flashcardRepository;
        this.storage = storage;
        this.extraction = extraction;
        this.generator = generator;
        this.localGenerator = localGenerator;
    }

    @Transactional
    public AiOverview process(Long materialId) {
        StudyMaterial material = material(materialId);
        byte[] bytes = storage.readBytes(material.getS3Key());
        MaterialExtractionService.ExtractedMaterial extracted = extraction.extract(bytes);

        // Bulk deletes execute immediately in PostgreSQL. This ordering makes
        // reprocessing idempotent and prevents the unique key
        // (material_id, chunk_index) from colliding with stale chunks.
        flashcardRepository.deleteAllByMaterialId(material.getId());
        profileRepository.deleteByMaterialId(material.getId());
        chunkRepository.deleteAllByMaterialId(material.getId());
        chunkRepository.flush();

        List<MaterialChunk> chunks = new ArrayList<>();
        for (int i = 0; i < extracted.chunks().size(); i++) {
            chunks.add(new MaterialChunk(material, i, extracted.chunks().get(i)));
        }
        chunkRepository.saveAll(chunks);

        String generationContext = chunks.stream()
                .limit(8)
                .map(MaterialChunk::getContent)
                .collect(Collectors.joining("\n\n"));

        BedrockStudyGenerator.StudyGuide bedrockGuide =
                generator.generateGuide(material.getTitle(), generationContext);

        String summary;
        String keyPoints;
        String generationMode;
        List<MaterialAiFlashcard> generatedCards;

        if (bedrockGuide.generatedByBedrock()) {
            summary = bedrockGuide.summary();
            keyPoints = bedrockGuide.keyPoints();
            generationMode = "BEDROCK";
            generatedCards = bedrockGuide.flashcards().stream()
                    .map(card -> new MaterialAiFlashcard(material, card.question(), card.answer()))
                    .toList();
        } else {
            LocalStudyGenerator.StudyGuide localGuide =
                    localGenerator.generateGuide(material.getTitle(), generationContext);
            summary = localGuide.summary();
            keyPoints = localGuide.keyPoints();
            generationMode = "LOCAL_FALLBACK";
            generatedCards = localGuide.flashcards().stream()
                    .map(card -> new MaterialAiFlashcard(material, card.question(), card.answer()))
                    .toList();
        }

        MaterialAiProfile profile = profileRepository.save(new MaterialAiProfile(
                material,
                summary,
                keyPoints,
                generationMode,
                chunks.size()
        ));

        if (!generatedCards.isEmpty()) {
            flashcardRepository.saveAll(generatedCards);
        }

        return overview(material, profile);
    }

    @Transactional(readOnly = true)
    public AiOverview overview(Long materialId) {
        StudyMaterial material = material(materialId);
        return profileRepository.findByMaterial(material)
                .map(profile -> overview(material, profile))
                .orElseGet(() -> new AiOverview(
                        false,
                        generator.isEnabled(),
                        "NOT_PROCESSED",
                        0,
                        null,
                        null,
                        List.of(),
                        null
                ));
    }

    @Transactional(readOnly = true)
    public AskResponse ask(Long materialId, String question) {
        StudyMaterial material = material(materialId);
        List<MaterialChunk> chunks = chunkRepository.findAllByMaterialOrderByChunkIndexAsc(material);

        if (chunks.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este material ainda não foi processado para IA"
            );
        }

        List<ScoredChunk> selected = retrieve(question, chunks);
        String context = selected.stream()
                .map(item -> "[Trecho " + (item.chunk().getChunkIndex() + 1) + "]\n" + item.chunk().getContent())
                .collect(Collectors.joining("\n\n"));

        BedrockStudyGenerator.AnswerResult bedrockAnswer =
                generator.answer(material.getTitle(), question, context);

        List<LocalStudyGenerator.Source> localSources = selected.stream()
                .map(item -> new LocalStudyGenerator.Source(
                        item.chunk().getChunkIndex() + 1,
                        item.chunk().getContent()
                ))
                .toList();

        String answer = bedrockAnswer.generatedByBedrock()
                ? bedrockAnswer.text()
                : localGenerator.answer(question, localSources);

        String generationMode = bedrockAnswer.generatedByBedrock()
                ? "BEDROCK"
                : "LOCAL_RAG";

        return new AskResponse(
                answer,
                bedrockAnswer.generatedByBedrock(),
                generationMode,
                selected.stream()
                        .map(item -> new SourceResponse(
                                item.chunk().getChunkIndex() + 1,
                                snippet(item.chunk().getContent())
                        ))
                        .toList()
        );
    }

    private List<ScoredChunk> retrieve(String question, List<MaterialChunk> chunks) {
        Set<String> terms = tokenize(question);

        List<ScoredChunk> scored = chunks.stream()
                .map(chunk -> new ScoredChunk(chunk, score(chunk.getContent(), terms)))
                .sorted(Comparator.comparingInt(ScoredChunk::score).reversed()
                        .thenComparingInt(item -> item.chunk().getChunkIndex()))
                .toList();

        List<ScoredChunk> positive = scored.stream()
                .filter(item -> item.score() > 0)
                .limit(4)
                .toList();

        return positive.isEmpty() ? scored.stream().limit(4).toList() : positive;
    }

    private int score(String content, Set<String> terms) {
        String normalized = normalize(content);
        int score = 0;
        for (String term : terms) {
            int index = 0;
            while ((index = normalized.indexOf(term, index)) >= 0) {
                score++;
                index += term.length();
            }
        }
        return score;
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

    private String snippet(String content) {
        String value = content.replaceAll("\\s+", " ").trim();
        return value.length() <= 520 ? value : value.substring(0, 517) + "...";
    }

    private AiOverview overview(StudyMaterial material, MaterialAiProfile profile) {
        List<AiFlashcardResponse> cards = flashcardRepository.findAllByMaterialOrderByIdAsc(material).stream()
                .map(card -> new AiFlashcardResponse(card.getId(), card.getQuestion(), card.getAnswer()))
                .toList();

        return new AiOverview(
                true,
                generator.isEnabled(),
                profile.getGenerationMode(),
                profile.getChunkCount(),
                profile.getSummary(),
                profile.getKeyPoints(),
                cards,
                profile.getProcessedAt().toString()
        );
    }

    private StudyMaterial material(Long id) {
        StudyMaterial material = materialRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material não encontrado"));

        if (!material.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Material não encontrado");
        }
        return material;
    }

    private record ScoredChunk(MaterialChunk chunk, int score) {}

    public record AiOverview(
            boolean processed,
            boolean bedrockEnabled,
            String generationMode,
            int chunkCount,
            String summary,
            String keyPoints,
            List<AiFlashcardResponse> flashcards,
            String processedAt
    ) {}

    public record AiFlashcardResponse(Long id, String question, String answer) {}
    public record SourceResponse(int chunkNumber, String excerpt) {}
    public record AskResponse(
            String answer,
            boolean generatedByBedrock,
            String generationMode,
            List<SourceResponse> sources
    ) {}
}
