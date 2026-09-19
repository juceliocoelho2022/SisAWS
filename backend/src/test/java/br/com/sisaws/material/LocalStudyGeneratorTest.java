package br.com.sisaws.material;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalStudyGeneratorTest {

    private final LocalStudyGenerator generator = new LocalStudyGenerator();

    @Test
    void shouldCreateLocalStudyGuideWithFlashcards() {
        String context = """
                Bucket: contêiner lógico no qual os objetos são armazenados.
                Objeto: unidade de dados armazenada no Amazon S3.
                O versionamento mantém diferentes versões de um mesmo objeto quando ele é atualizado.
                A replicação copia objetos entre buckets conforme regras configuradas.
                Uma URL pré-assinada concede acesso temporário a um objeto sem tornar o bucket público.
                """;

        LocalStudyGenerator.StudyGuide guide = generator.generateGuide("Amazon S3", context);

        assertFalse(guide.summary().isBlank());
        assertFalse(guide.keyPoints().isBlank());
        assertFalse(guide.flashcards().isEmpty());
    }

    @Test
    void shouldAnswerOnlyFromRetrievedSourcesAndCiteChunk() {
        String answer = generator.answer(
                "Qual é a diferença entre versionamento e replicação?",
                List.of(
                        new LocalStudyGenerator.Source(
                                4,
                                "O versionamento mantém diferentes versões de um mesmo objeto. A replicação copia objetos entre buckets conforme regras configuradas."
                        )
                )
        );

        assertTrue(answer.contains("versionamento"));
        assertTrue(answer.contains("replicação"));
        assertTrue(answer.contains("[Trecho 4]"));
    }
}
