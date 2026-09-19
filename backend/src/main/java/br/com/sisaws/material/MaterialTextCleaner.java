package br.com.sisaws.material;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

final class MaterialTextCleaner {

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

    private MaterialTextCleaner() {}

    static String cleanForStudy(String text) {
        if (text == null || text.isBlank()) return "";

        return text
                .replaceAll("(?i)SisAWS\\s*-\\s*Material original para teste técnico e acadêmico\\s+Página\\s+\\d+", " ")
                .replaceAll("(?i)Material de Teste\\s*-\\s*Biblioteca Acadêmica SisAWS", " ")
                .replaceAll("(?i)Objetivo\\s+Validar upload, extração de texto, chunking, RAG e geração com Amazon Bedrock", " ")
                .replaceAll("(?i)Formato\\s+PDF com texto selecionável", " ")
                .replaceAll("(?i)Serviço AWS\\s+Amazon S3", " ")
                .replaceAll("(?i)Nível\\s+Fundamentos", " ")
                .replaceAll("(?i)Uso\\s+Teste técnico do módulo SisAWS AI Study", " ")
                .replaceAll("(?i)Este documento foi criado exclusivamente para teste do SisAWS\\.", " ")
                .replaceAll("(?i)Pergunta sugerida para o teste:\\s*[“\"]?[^?]{0,240}\\?[”\"]?", " ")
                .replaceAll("(?i)\\bPágina\\s+\\d+\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    static boolean isBoilerplate(String value) {
        String normalized = normalize(value)
                .replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return BOILERPLATE_MARKERS.stream().anyMatch(normalized::contains);
    }

    static int rankingPenalty(String value) {
        String normalized = normalize(value);
        int penalty = 0;

        if (normalized.contains("pergunta sugerida para o teste")) penalty += 6;
        if (normalized.contains("objetivo validar upload")) penalty += 3;
        if (normalized.contains("material de teste biblioteca academica sisaws")) penalty += 2;
        if (normalized.contains("material original para teste tecnico e academico")) penalty += 1;
        if (normalized.contains("uso teste tecnico")) penalty += 1;

        return penalty;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
