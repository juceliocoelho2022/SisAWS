package br.com.sisaws.material;

import br.com.sisaws.user.AppUser;
import br.com.sisaws.user.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/materials")
public class StudyMaterialController {

    private final StudyMaterialService service;

    public StudyMaterialController(StudyMaterialService service) {
        this.service = service;
    }

    @GetMapping
    public List<StudyMaterialService.MaterialSummary> list(Authentication authentication) {
        return service.list(user(authentication));
    }

    @GetMapping("/{materialId}")
    public StudyMaterialService.MaterialDetail detail(@PathVariable Long materialId, Authentication authentication) {
        return service.detail(user(authentication), materialId);
    }

    @GetMapping("/{materialId}/access")
    public StudyMaterialService.AccessResponse access(@PathVariable Long materialId) {
        return service.access(materialId);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public StudyMaterialService.MaterialDetail upload(
            Authentication authentication,
            @RequestPart("file") MultipartFile file,
            @RequestParam @NotBlank @Size(max = 220) String title,
            @RequestParam @NotBlank @Size(max = 180) String author,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String publisher,
            @RequestParam(required = false) String edition,
            @RequestParam(required = false) Integer publicationYear,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) String sourceUrl,
            @RequestParam @NotNull MaterialLicense licenseType,
            @RequestParam @NotBlank String awsService,
            @RequestParam @NotBlank String saaDomain,
            @RequestParam(defaultValue = "30") @Min(1) int estimatedMinutes) {

        AppUser instructor = requireInstructor(authentication);

        return service.upload(instructor, file, new StudyMaterialService.UploadCommand(
                title, description, author, publisher, edition, publicationYear, isbn, sourceUrl,
                licenseType, awsService, saaDomain, estimatedMinutes
        ));
    }

    @PostMapping("/{materialId}/chapters")
    @ResponseStatus(HttpStatus.CREATED)
    public StudyMaterialService.MaterialDetail addChapter(
            @PathVariable Long materialId,
            @Valid @RequestBody ChapterRequest request,
            Authentication authentication) {

        AppUser instructor = requireInstructor(authentication);

        return service.addChapter(instructor, materialId, new StudyMaterialService.ChapterCommand(
                request.chapterNumber(),
                request.title(),
                request.description(),
                request.awsService(),
                request.saaDomain(),
                request.estimatedMinutes()
        ));
    }

    @PostMapping("/{materialId}/chapters/{chapterId}/complete")
    public StudyMaterialService.MaterialDetail completeChapter(
            @PathVariable Long materialId,
            @PathVariable Long chapterId,
            Authentication authentication) {
        return service.completeChapter(user(authentication), materialId, chapterId);
    }

    private AppUser user(Authentication authentication) {
        return (AppUser) authentication.getPrincipal();
    }

    private AppUser requireInstructor(Authentication authentication) {
        AppUser user = user(authentication);
        if (user.getRole() != Role.INSTRUCTOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas instrutores podem publicar materiais");
        }
        return user;
    }

    public record ChapterRequest(
            @Min(1) int chapterNumber,
            @NotBlank @Size(max = 220) String title,
            String description,
            @NotBlank String awsService,
            @NotBlank String saaDomain,
            @Min(1) int estimatedMinutes
    ) {}
}
