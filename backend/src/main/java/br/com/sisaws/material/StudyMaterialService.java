package br.com.sisaws.material;

import br.com.sisaws.user.AppUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudyMaterialService {

    private final StudyMaterialRepository materialRepository;
    private final StudyChapterRepository chapterRepository;
    private final MaterialProgressRepository progressRepository;
    private final MaterialStorageService storage;

    public StudyMaterialService(StudyMaterialRepository materialRepository,
                                StudyChapterRepository chapterRepository,
                                MaterialProgressRepository progressRepository,
                                MaterialStorageService storage) {
        this.materialRepository = materialRepository;
        this.chapterRepository = chapterRepository;
        this.progressRepository = progressRepository;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<MaterialSummary> list(AppUser user) {
        Map<Long, Set<Long>> completedByMaterial = completedChapters(user);

        return materialRepository.findAllByActiveTrueOrderByCreatedAtDesc().stream()
                .map(material -> {
                    List<StudyChapter> chapters = chapterRepository.findAllByMaterialOrderByChapterNumberAsc(material);
                    int completed = completedByMaterial.getOrDefault(material.getId(), Set.of()).size();
                    int percent = chapters.isEmpty() ? 0 : (int) Math.round(completed * 100.0 / chapters.size());
                    return summary(material, chapters.size(), completed, percent);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public MaterialDetail detail(AppUser user, Long materialId) {
        StudyMaterial material = activeMaterial(materialId);
        List<StudyChapter> chapters = chapterRepository.findAllByMaterialOrderByChapterNumberAsc(material);
        Set<Long> completed = completedChapters(user).getOrDefault(material.getId(), Set.of());
        int percent = chapters.isEmpty() ? 0 : (int) Math.round(completed.size() * 100.0 / chapters.size());

        return new MaterialDetail(
                summary(material, chapters.size(), completed.size(), percent),
                material.getDescription(),
                material.getPublisher(),
                material.getEdition(),
                material.getPublicationYear(),
                material.getIsbn(),
                material.getSourceUrl(),
                chapters.stream().map(chapter -> new ChapterResponse(
                        chapter.getId(),
                        chapter.getChapterNumber(),
                        chapter.getTitle(),
                        chapter.getDescription(),
                        chapter.getAwsService(),
                        chapter.getSaaDomain(),
                        chapter.getEstimatedMinutes(),
                        completed.contains(chapter.getId())
                )).toList()
        );
    }

    @Transactional
    public MaterialDetail upload(AppUser instructor, MultipartFile file, UploadCommand command) {
        MaterialStorageService.StoredObject stored = storage.upload(file);

        try {
            StudyMaterial material = materialRepository.save(new StudyMaterial(
                    command.title(),
                    command.description(),
                    command.author(),
                    command.publisher(),
                    command.edition(),
                    command.publicationYear(),
                    command.isbn(),
                    command.sourceUrl(),
                    stored.materialType(),
                    command.licenseType(),
                    command.awsService(),
                    command.saaDomain(),
                    command.estimatedMinutes(),
                    stored.key(),
                    stored.contentType(),
                    stored.size(),
                    instructor
            ));

            chapterRepository.save(new StudyChapter(
                    material,
                    1,
                    "Material completo",
                    "Leitura inicial do material publicado.",
                    command.awsService(),
                    command.saaDomain(),
                    command.estimatedMinutes()
            ));

            return detail(instructor, material.getId());
        } catch (RuntimeException exception) {
            storage.deleteQuietly(stored.key());
            throw exception;
        }
    }

    @Transactional
    public MaterialDetail addChapter(AppUser user, Long materialId, ChapterCommand command) {
        StudyMaterial material = activeMaterial(materialId);

        if (chapterRepository.existsByMaterialAndChapterNumber(material, command.chapterNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um capítulo com este número");
        }

        chapterRepository.save(new StudyChapter(
                material,
                command.chapterNumber(),
                command.title(),
                command.description(),
                command.awsService(),
                command.saaDomain(),
                command.estimatedMinutes()
        ));

        return detail(user, materialId);
    }

    @Transactional
    public MaterialDetail completeChapter(AppUser user, Long materialId, Long chapterId) {
        StudyMaterial material = activeMaterial(materialId);
        StudyChapter chapter = chapterRepository.findByIdAndMaterial(chapterId, material)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Capítulo não encontrado"));

        MaterialProgress progress = progressRepository.findByUserAndChapter(user, chapter)
                .orElseGet(() -> new MaterialProgress(user, chapter));
        progress.complete();
        progressRepository.save(progress);

        return detail(user, materialId);
    }

    @Transactional(readOnly = true)
    public AccessResponse access(Long materialId) {
        StudyMaterial material = activeMaterial(materialId);
        return new AccessResponse(storage.createReadUrl(material.getS3Key()), 600);
    }

    private StudyMaterial activeMaterial(Long id) {
        StudyMaterial material = materialRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material não encontrado"));

        if (!material.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Material não encontrado");
        }
        return material;
    }

    private Map<Long, Set<Long>> completedChapters(AppUser user) {
        return progressRepository.findAllByUser(user).stream()
                .filter(progress -> progress.getProgressPercent() == 100)
                .collect(Collectors.groupingBy(
                        progress -> progress.getChapter().getMaterial().getId(),
                        Collectors.mapping(progress -> progress.getChapter().getId(), Collectors.toSet())
                ));
    }

    private MaterialSummary summary(StudyMaterial material, int chapters, int completed, int progressPercent) {
        return new MaterialSummary(
                material.getId(),
                material.getTitle(),
                material.getAuthor(),
                material.getMaterialType().name(),
                material.getLicenseType().name(),
                material.getAwsService(),
                material.getSaaDomain(),
                material.getEstimatedMinutes(),
                material.getFileSize(),
                chapters,
                completed,
                progressPercent
        );
    }

    public record UploadCommand(
            String title,
            String description,
            String author,
            String publisher,
            String edition,
            Integer publicationYear,
            String isbn,
            String sourceUrl,
            MaterialLicense licenseType,
            String awsService,
            String saaDomain,
            int estimatedMinutes
    ) {}

    public record ChapterCommand(
            int chapterNumber,
            String title,
            String description,
            String awsService,
            String saaDomain,
            int estimatedMinutes
    ) {}

    public record MaterialSummary(
            Long id,
            String title,
            String author,
            String materialType,
            String licenseType,
            String awsService,
            String saaDomain,
            int estimatedMinutes,
            long fileSize,
            int chapterCount,
            int completedChapters,
            int progressPercent
    ) {}

    public record MaterialDetail(
            MaterialSummary material,
            String description,
            String publisher,
            String edition,
            Integer publicationYear,
            String isbn,
            String sourceUrl,
            List<ChapterResponse> chapters
    ) {}

    public record ChapterResponse(
            Long id,
            int chapterNumber,
            String title,
            String description,
            String awsService,
            String saaDomain,
            int estimatedMinutes,
            boolean completed
    ) {}

    public record AccessResponse(String url, int expiresInSeconds) {}
}
