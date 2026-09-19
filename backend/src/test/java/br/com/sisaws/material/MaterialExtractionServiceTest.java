package br.com.sisaws.material;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MaterialExtractionServiceTest {

    @Test
    void shouldSplitLongTextIntoOverlappingChunks() {
        MaterialExtractionService service = new MaterialExtractionService();
        String text = ("Amazon S3 armazena objetos com alta durabilidade. "
                + "IAM controla identidades e permissões. "
                + "VPC isola recursos em redes virtuais. ").repeat(80);

        List<String> chunks = service.chunkText(text);

        assertTrue(chunks.size() > 2);
        assertTrue(chunks.stream().allMatch(chunk -> chunk.length() <= 1800));
        assertTrue(chunks.getFirst().contains("Amazon S3"));
    }

    @Test
    void shouldKeepShortTextInSingleChunk() {
        MaterialExtractionService service = new MaterialExtractionService();

        List<String> chunks = service.chunkText("Amazon S3 é um serviço de armazenamento de objetos.");

        assertEquals(1, chunks.size());
    }
}
