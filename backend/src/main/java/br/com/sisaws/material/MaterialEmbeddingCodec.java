package br.com.sisaws.material;

import java.util.Optional;

final class MaterialEmbeddingCodec {

    private MaterialEmbeddingCodec() {}

    static String encode(float[] vector) {
        if (vector == null || vector.length == 0) return null;

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) builder.append(',');
            builder.append(vector[i]);
        }
        return builder.toString();
    }

    static Optional<float[]> decode(String value) {
        if (value == null || value.isBlank()) return Optional.empty();

        String[] parts = value.split(",");
        float[] vector = new float[parts.length];

        try {
            for (int i = 0; i < parts.length; i++) {
                vector[i] = Float.parseFloat(parts[i]);
            }
            return Optional.of(vector);
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
