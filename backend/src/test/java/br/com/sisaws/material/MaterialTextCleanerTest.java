package br.com.sisaws.material;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MaterialTextCleanerTest {

    @Test
    void shouldRemoveTestBoilerplateButKeepAcademicContent() {
        String raw = """
                SisAWS - Material original para teste técnico e acadêmico Página 1
                Material de Teste - Biblioteca Acadêmica SisAWS
                Objetivo Validar upload, extração de texto, chunking, RAG e geração com Amazon Bedrock
                Formato PDF com texto selecionável
                Pergunta sugerida para o teste: “Segundo este material, qual é a diferença entre versionamento e replicação no Amazon S3?”
                O versionamento mantém diferentes versões de um mesmo objeto.
                A replicação copia objetos para outro bucket.
                """;

        String cleaned = MaterialTextCleaner.cleanForStudy(raw);

        assertFalse(cleaned.contains("Pergunta sugerida"));
        assertFalse(cleaned.contains("Validar upload"));
        assertFalse(cleaned.contains("Página 1"));
        assertTrue(cleaned.contains("versionamento"));
        assertTrue(cleaned.contains("replicação"));
    }

    @Test
    void shouldPenalizeChunksDominatedByTestMetadata() {
        int metadataPenalty = MaterialTextCleaner.rankingPenalty(
                "Pergunta sugerida para o teste: diferença entre versionamento e replicação"
        );
        int contentPenalty = MaterialTextCleaner.rankingPenalty(
                "Versionamento preserva versões anteriores. Replicação copia objetos."
        );

        assertTrue(metadataPenalty > contentPenalty);
        assertEquals(0, contentPenalty);
    }
}
