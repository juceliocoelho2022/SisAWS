package br.com.sisaws.material;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class LocalStudyGenerator {

    private static final Pattern DEFINITION_PATTERN = Pattern.compile(
            "(?m)([A-ZÁÉÍÓÚÂÊÔÃÕÇ][A-Za-zÀ-ÿ0-9 /-]{2,45}):\\s*([^\\n.!?]{20,260}[.!?]?)"
    );

    private static final Set<String> STOP_WORDS = Set.of(
            "para", "com", "uma", "uns", "das", "dos", "que", "por", "como", "qual",
            "quais", "sobre", "este", "esta", "isso", "ser", "tem", "sao", "mais", "aws",
            "segundo", "material", "entre"
    );

    private static final List<String> BOILERPLATE_MARKERS = List.of(
            "material original para teste tecnico e academico",
            "material de teste biblioteca academica sisaws",
            "objetivo validar upload",
            "formato pdf com texto selecionavel",
            "nivel fundamentos",
            "uso teste tecnico",
            "criado exclusivamente para teste do sisaws",
            "pergunta sugerida para o teste"
    );

    private static final List<String> ACADEMIC_CONCEPTS = List.of(
            "amazon s3", "bucket", "objeto", "chave", "metadados", "versionamento",
            "replicacao", "lifecycle", "criptografia", "url pre-assinada",
            "storage class", "classe de armazenamento"
    );

    public StudyGuide generateGuide(String title, String context) {
        List<String> sentences = meaningfulSentences(context);

        List<String> academicSentences = sentences.stream()
                .filter(this::containsAcademicConcept)
                .toList();

        List<String> summarySource = academicSentences.isEmpty() ? sentences : academicSentences;

        String summary = summarySource.stream()
                .limit(4)
                .collect(Collectors.joining(" "));

        if (summary.isBlank()) {
            summary = "O material foi indexado e está disponível para consulta contextual.";
        }

        String keyPoints = summarySource.stream()
                .limit(6)
                .map(sentence -> "• " + sentence)
                .collect(Collectors.joining("\n"));

        if (keyPoints.isBlank()) {
            keyPoints = "• Consulte os trechos recuperados para revisar os conceitos do material.";
        }

        List<Flashcard> flashcards = definitionFlashcards(context);

        if (flashcards.size() < 3) {
            for (String sentence : summarySource) {
                if (flashcards.size() >= 5) break;
                String subject = inferSubject(sentence);
                if (subject == null || isBoilerplate(subject)) continue;

                String question = "O que o material destaca sobre " + subject + "?";
                boolean duplicate = flashcards.stream()
                        .anyMatch(card -> card.question().equalsIgnoreCase(question));

                if (!duplicate) {
                    flashcards.add(new Flashcard(question, sentence));
                }
            }
        }

        return new StudyGuide(
                "Resumo extrativo local — " + title + ": " + summary,
                keyPoints,
                flashcards.stream().limit(5).toList()
        );
    }

    public String answer(String question, List<Source> sources) {
        Set<String> terms = tokens(question);
        List<SentenceCandidate> candidates = collectCandidates(sources, terms);

        List<SentenceCandidate> selected = selectEvidence(question, candidates);

        if (selected.isEmpty() && !sources.isEmpty()) {
            String fallback = meaningfulSentences(sources.getFirst().content()).stream()
                    .findFirst()
                    .orElse(cleanText(sources.getFirst().content()).trim());
            selected = List.of(new SentenceCandidate(sources.getFirst().chunkNumber(), fallback, 0));
        }

        if (selected.isEmpty()) {
            return "Não encontrei conteúdo suficiente no material para responder com segurança.";
        }

        return "Com base exclusivamente no material: " + selected.stream()
                .map(item -> item.sentence() + " [Trecho " + item.chunkNumber() + "]")
                .collect(Collectors.joining(" "));
    }

    private List<SentenceCandidate> collectCandidates(List<Source> sources, Set<String> terms) {
        List<SentenceCandidate> candidates = new ArrayList<>();

        for (Source source : sources) {
            for (String sentence : meaningfulSentences(source.content())) {
                if (looksLikeQuestion(sentence)) continue;

                int score = overlapScore(sentence, terms);
                if (score > 0) {
                    candidates.add(new SentenceCandidate(source.chunkNumber(), sentence, score));
                }
            }
        }

        candidates.sort(Comparator
                .comparingInt(SentenceCandidate::score).reversed()
                .thenComparingInt(SentenceCandidate::chunkNumber));

        return candidates;
    }

    private List<SentenceCandidate> selectEvidence(String question, List<SentenceCandidate> candidates) {
        if (candidates.isEmpty()) return List.of();

        List<String> concepts = requestedConcepts(question);

        if (concepts.size() >= 2) {
            List<SentenceCandidate> comparative = new ArrayList<>();

            for (String concept : concepts) {
                candidates.stream()
                        .filter(candidate -> normalize(candidate.sentence()).contains(concept))
                        .findFirst()
                        .ifPresent(candidate -> {
                            boolean duplicate = comparative.stream().anyMatch(existing ->
                                    existing.chunkNumber() == candidate.chunkNumber()
                                            && existing.sentence().equals(candidate.sentence()));
                            if (!duplicate) comparative.add(candidate);
                        });
            }

            if (comparative.size() >= 2 || coversAllConcepts(comparative, concepts)) {
                return comparative.stream().limit(4).toList();
            }
        }

        return candidates.stream().limit(3).toList();
    }

    private List<String> requestedConcepts(String question) {
        String normalized = normalize(question);
        return ACADEMIC_CONCEPTS.stream()
                .filter(normalized::contains)
                .distinct()
                .toList();
    }

    private boolean coversAllConcepts(List<SentenceCandidate> selected, List<String> concepts) {
        String joined = selected.stream()
                .map(SentenceCandidate::sentence)
                .map(this::normalize)
                .collect(Collectors.joining(" "));
        return concepts.stream().allMatch(joined::contains);
    }

    private boolean looksLikeQuestion(String sentence) {
        String normalized = normalize(sentence).trim();
        return sentence.contains("?")
                || normalized.startsWith("qual ")
                || normalized.startsWith("quais ")
                || normalized.startsWith("pergunta sugerida");
    }

    private List<Flashcard> definitionFlashcards(String context) {
        List<Flashcard> cards = new ArrayList<>();
        Matcher matcher = DEFINITION_PATTERN.matcher(context);

        while (matcher.find() && cards.size() < 5) {
            String term = matcher.group(1)
                    .replace("•", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            String definition = matcher.group(2)
                    .replace("•", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            if (term.length() > 45 || definition.length() < 20) continue;
            if (isBoilerplate(term) || isBoilerplate(definition)) continue;
            if (!containsAcademicConcept(term + " " + definition)) continue;

            String question = "O que o material define como " + term + "?";
            boolean duplicate = cards.stream()
                    .anyMatch(card -> card.question().equalsIgnoreCase(question));

            if (!duplicate) {
                cards.add(new Flashcard(question, definition));
            }
        }

        return cards;
    }

    private List<String> meaningfulSentences(String text) {
        return Arrays.stream(cleanText(text)
                        .replaceAll("[\\r\\t]+", " ")
                        .replace("•", ". ")
                        .replaceAll("\\s+", " ")
                        .split("(?<=[.!?])\\s+"))
                .map(String::trim)
                .map(sentence -> sentence.replaceAll("^[.\\-–— ]+", "").trim())
                .filter(sentence -> sentence.length() >= 35)
                .filter(sentence -> sentence.length() <= 420)
                .filter(sentence -> !isBoilerplate(sentence))
                .distinct()
                .toList();
    }

    private String cleanText(String text) {
        if (text == null) return "";

        return text
                .replaceAll("(?i)SisAWS\\s*-\\s*Material original para teste técnico e acadêmico\\s+Página\\s+\\d+", " ")
                .replaceAll("(?i)Material de Teste\\s*-\\s*Biblioteca Acadêmica SisAWS", " ")
                .replaceAll("(?i)Objetivo\\s+Validar upload, extração de texto, chunking, RAG e geração com Amazon Bedrock", " ")
                .replaceAll("(?i)Formato\\s+PDF com texto selecionável", " ")
                .replaceAll("(?i)Serviço AWS\\s+Amazon S3", " ")
                .replaceAll("(?i)Nível\\s+Fundamentos", " ")
                .replaceAll("(?i)Uso\\s+Teste técnico do módulo SisAWS AI Study", " ")
                .replaceAll("(?i)Este documento foi criado exclusivamente para teste do SisAWS\\.", " ")
                .replaceAll("(?i)Pergunta sugerida para o teste:\\s*[“\"]?[^?]{0,240}\\?[”\"]?", " ");
    }

    private boolean isBoilerplate(String value) {
        String normalized = normalize(value).replaceAll("[^a-z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
        return BOILERPLATE_MARKERS.stream().anyMatch(normalized::contains);
    }

    private boolean containsAcademicConcept(String value) {
        String normalized = normalize(value);
        return ACADEMIC_CONCEPTS.stream().anyMatch(normalized::contains);
    }

    private String inferSubject(String sentence) {
        String cleaned = sentence.replaceAll("^[0-9. ]+", "").trim();
        int limit = Math.min(cleaned.length(), 70);
        String prefix = cleaned.substring(0, limit);
        int boundary = prefix.indexOf(" é ");
        if (boundary < 0) boundary = prefix.indexOf(" pode ");
        if (boundary < 0) boundary = prefix.indexOf(" permite ");
        if (boundary < 0) return null;

        String subject = prefix.substring(0, boundary).trim();
        return subject.length() >= 3 && subject.length() <= 55 ? subject : null;
    }

    private int overlapScore(String sentence, Set<String> terms) {
        String normalized = normalize(sentence);
        int score = 0;
        for (String term : terms) {
            if (normalized.contains(term)) score++;
        }
        return score;
    }

    private Set<String> tokens(String value) {
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

    public record StudyGuide(String summary, String keyPoints, List<Flashcard> flashcards) {}
    public record Flashcard(String question, String answer) {}
    public record Source(int chunkNumber, String content) {}
    private record SentenceCandidate(int chunkNumber, String sentence, int score) {}
}
