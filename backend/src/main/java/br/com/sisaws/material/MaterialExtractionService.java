package br.com.sisaws.material;

import org.apache.tika.Tika;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class MaterialExtractionService {

    private static final int CHUNK_SIZE = 1800;
    private static final int OVERLAP = 250;

    private final Tika tika;

    public MaterialExtractionService() {
        this.tika = new Tika();
        this.tika.setMaxStringLength(2_000_000);
    }

    public ExtractedMaterial extract(byte[] bytes) {
        try {
            String raw = tika.parseToString(new ByteArrayInputStream(bytes));
            String normalized = normalize(raw);

            if (normalized.length() < 80) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "O material não possui texto suficiente para indexação. PDFs somente com imagem exigem OCR."
                );
            }

            return new ExtractedMaterial(normalized, chunkText(normalized));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não foi possível extrair o texto deste material",
                    exception
            );
        }
    }

    List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(text.length(), start + CHUNK_SIZE);

            if (end < text.length()) {
                int sentence = Math.max(
                        text.lastIndexOf(". ", end),
                        Math.max(text.lastIndexOf("\n", end), text.lastIndexOf("; ", end))
                );
                if (sentence > start + 900) {
                    end = sentence + 1;
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            if (end >= text.length()) break;
            start = Math.max(start + 1, end - OVERLAP);
        }

        return chunks;
    }

    private String normalize(String text) {
        return text
                .replace("\u0000", "")
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll(" +", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    public record ExtractedMaterial(String text, List<String> chunks) {}
}
