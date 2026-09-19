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

    public StudyGuide generateGuide(String title, String context) {
        List<String> sentences = meaningfulSentences(context);

        String summary = sentences.stream()
                .limit(4)
                .collect(Collectors.joining(" "));

        if (summary.isBlank()) {
            summary = "O material foi indexado e está disponível para consulta contextual.";
        }

        String keyPoints = sentences.stream()
                .skip(Math.min(2, sentences.size()))
                .limit(6)
                .map(sentence -> "• " + sentence)
                .collect(Collectors.joining("\n"));

        if (keyPoints.isBlank()) {
            keyPoints = "• Consulte os trechos recuperados para revisar os conceitos do material.";
        }

        List<Flashcard> flashcards = definitionFlashcards(context);

        if (flashcards.size() < 3) {
            for (String sentence : sentences) {
                if (flashcards.size() >= 5) break;
                String subject = inferSubject(sentence);
                if (subject == null) continue;

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
        List<SentenceCandidate> candidates = new ArrayList<>();

        for (Source source : sources) {
            for (String sentence : meaningfulSentences(source.content())) {
                int score = overlapScore(sentence, terms);
                if (score > 0) {
                    candidates.add(new SentenceCandidate(source.chunkNumber(), sentence, score));
                }
            }
        }

        candidates.sort(Comparator
                .comparingInt(SentenceCandidate::score).reversed()
                .thenComparingInt(SentenceCandidate::chunkNumber));

        List<SentenceCandidate> selected = candidates.stream()
                .limit(3)
                .toList();

        if (selected.isEmpty() && !sources.isEmpty()) {
            String fallback = meaningfulSentences(sources.getFirst().content()).stream()
                    .findFirst()
                    .orElse(sources.getFirst().content());
            selected = List.of(new SentenceCandidate(sources.getFirst().chunkNumber(), fallback, 0));
        }

        if (selected.isEmpty()) {
            return "Não encontrei conteúdo suficiente no material para responder com segurança.";
        }

        return "Com base exclusivamente no material: " + selected.stream()
                .map(item -> item.sentence() + " [Trecho " + item.chunkNumber() + "]")
                .collect(Collectors.joining(" "));
    }

    private List<Flashcard> definitionFlashcards(String context) {
        List<Flashcard> cards = new ArrayList<>();
        Matcher matcher = DEFINITION_PATTERN.matcher(context);

        while (matcher.find() && cards.size() < 5) {
            String term = matcher.group(1).replaceAll("\\s+", " ").trim();
            String definition = matcher.group(2).replaceAll("\\s+", " ").trim();

            if (term.length() > 45 || definition.length() < 20) continue;

            cards.add(new Flashcard(
                    "O que o material define como " + term + "?",
                    definition
            ));
        }

        return cards;
    }

    private List<String> meaningfulSentences(String text) {
        return Arrays.stream(text
                        .replaceAll("[\\r\\t]+", " ")
                        .replaceAll("\\s+", " ")
                        .split("(?<=[.!?])\\s+"))
                .map(String::trim)
                .filter(sentence -> sentence.length() >= 45)
                .filter(sentence -> sentence.length() <= 420)
                .distinct()
                .toList();
    }

    private String inferSubject(String sentence) {
        String cleaned = sentence.replaceAll("^[0-9. ]+", "").trim();
        int limit = Math.min(cleaned.length(), 55);
        String prefix = cleaned.substring(0, limit);
        int boundary = prefix.indexOf(" é ");
        if (boundary < 0) boundary = prefix.indexOf(" pode ");
        if (boundary < 0) boundary = prefix.indexOf(" permite ");
        if (boundary < 0) return null;

        String subject = prefix.substring(0, boundary).trim();
        return subject.length() >= 3 && subject.length() <= 45 ? subject : null;
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
