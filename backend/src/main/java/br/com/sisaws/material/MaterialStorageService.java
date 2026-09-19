package br.com.sisaws.material;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class MaterialStorageService {

    private static final Map<String, MaterialType> TYPES = Map.of(
            "pdf", MaterialType.PDF,
            "docx", MaterialType.DOCX,
            "epub", MaterialType.EPUB
    );

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "pdf", "application/pdf",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "epub", "application/epub+zip"
    );

    private final String bucket;
    private final long maxBytes;
    private final S3Client s3;
    private final S3Presigner presigner;

    public MaterialStorageService(
            @Value("${sisaws.security.library.bucket:}") String bucket,
            @Value("${sisaws.security.library.aws-region:sa-east-1}") String awsRegion,
            @Value("${sisaws.security.library.max-file-size-mb:25}") long maxFileSizeMb) {
        this.bucket = bucket == null ? "" : bucket.trim();
        this.maxBytes = maxFileSizeMb * 1024L * 1024L;
        Region region = Region.of(awsRegion);
        this.s3 = S3Client.builder().region(region).build();
        this.presigner = S3Presigner.builder().region(region).build();
    }

    public StoredObject upload(MultipartFile file) {
        requireConfigured();

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione um arquivo para publicar");
        }
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "O material excede o limite permitido");
        }

        String extension = extension(file.getOriginalFilename());
        MaterialType materialType = TYPES.get(extension);
        if (materialType == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato permitido: PDF, DOCX ou EPUB");
        }

        String key = "materials/" + UUID.randomUUID() + "." + extension;
        String contentType = CONTENT_TYPES.get(extension);

        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .serverSideEncryption(ServerSideEncryption.AES256)
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível ler o arquivo");
        }

        return new StoredObject(key, materialType, contentType, file.getSize());
    }

    public byte[] readBytes(String key) {
        requireConfigured();
        return s3.getObjectAsBytes(
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build()
        ).asByteArray();
    }

    public String createReadUrl(String key) {
        requireConfigured();

        GetObjectRequest getObject = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        return presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10))
                        .getObjectRequest(getObject)
                        .build()
        ).url().toString();
    }

    public void deleteQuietly(String key) {
        if (bucket.isBlank() || key == null || key.isBlank()) return;
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (RuntimeException ignored) {
            // Best-effort cleanup when database persistence fails after upload.
        }
    }

    private void requireConfigured() {
        if (bucket.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Biblioteca S3 ainda não foi provisionada");
        }
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    public record StoredObject(String key, MaterialType materialType, String contentType, long size) {}
}
