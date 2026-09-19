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
    void shouldIgnoreTestMetadataAndSuggestedQuestion() {
        String context = """
                SisAWS - Material original para teste técnico e acadêmico Página 1
                Fundamentos do Amazon S3
                Objetivo Validar upload, extração de texto, chunking, RAG e geração com Amazon Bedrock
                Formato PDF com texto selecionável
                Serviço AWS Amazon S3
                Nível Fundamentos
                Uso Teste técnico do módulo SisAWS AI Study
                Este documento foi criado exclusivamente para teste do SisAWS.
                Pergunta sugerida para o teste: “Segundo este material, qual é a diferença entre versionamento e replicação no Amazon S3?”
                O Amazon Simple Storage Service (Amazon S3) é um serviço de armazenamento de objetos.
                Bucket: contêiner lógico no qual os objetos são armazenados.
                Objeto: unidade de dados armazenada no S3.
                Versionamento: preserva versões anteriores de um objeto armazenado.
                Replicação: copia objetos entre buckets conforme regras configuradas.
                """;

        LocalStudyGenerator.StudyGuide guide = generator.generateGuide("Fundamentos do Amazon S3", context);

        assertFalse(guide.summary().contains("Pergunta sugerida"));
        assertFalse(guide.summary().contains("Validar upload"));
        assertTrue(guide.summary().contains("Amazon S3"));
        assertTrue(guide.flashcards().stream()
                .noneMatch(card -> card.question().contains("Pergunta sugerida")));
        assertTrue(guide.flashcards().stream()
                .anyMatch(card -> card.question().contains("Bucket")));
    }

    @Test
    void shouldCompareRequestedConceptsUsingEvidenceForBoth() {
        String answer = generator.answer(
                "Qual é a diferença entre versionamento e replicação?",
                List.of(
                        new LocalStudyGenerator.Source(
                                1,
                                "Pergunta sugerida para o teste: Qual é a diferença entre versionamento e replicação?"
                        ),
                        new LocalStudyGenerator.Source(
                                3,
                                "O versionamento mantém diferentes versões de um mesmo objeto quando ele é substituído ou atualizado."
                        ),
                        new LocalStudyGenerator.Source(
                                4,
                                "A replicação copia objetos para outro bucket conforme regras configuradas."
                        )
                )
        );

        assertFalse(answer.contains("Pergunta sugerida"));
        assertTrue(answer.contains("versionamento"));
        assertTrue(answer.toLowerCase().contains("replicação"));
        assertTrue(answer.contains("[Trecho 3]"));
        assertTrue(answer.contains("[Trecho 4]"));
    }

    @Test
    void shouldNotTreatSectionHeadingAsReplicationEvidence() {
        String answer = generator.answer(
                "Qual é a diferença entre versionamento e replicação?",
                List.of(
                        new LocalStudyGenerator.Source(
                                3,
                                "Versionamento, replicação e ciclo de vida 3.1 Versionamento. " +
                                "O versionamento mantém diferentes versões de um mesmo objeto quando ele é atualizado."
                        ),
                        new LocalStudyGenerator.Source(
                                4,
                                "Replicação: copiar objetos para outro bucket e manter uma cópia em outro destino."
                        )
                )
        );

        assertTrue(answer.contains("versionamento"));
        assertTrue(answer.contains("replicação"));
        assertTrue(answer.contains("[Trecho 3]"));
        assertTrue(answer.contains("[Trecho 4]"));
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
