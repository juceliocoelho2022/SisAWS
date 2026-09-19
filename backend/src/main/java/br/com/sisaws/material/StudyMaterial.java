package br.com.sisaws.material;

import br.com.sisaws.user.AppUser;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_materials")
public class StudyMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 220)
    private String title;

    @Column(length = 1500)
    private String description;

    @Column(nullable = false, length = 180)
    private String author;

    @Column(length = 180)
    private String publisher;

    @Column(length = 80)
    private String edition;

    private Integer publicationYear;

    @Column(length = 32)
    private String isbn;

    @Column(length = 500)
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MaterialType materialType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MaterialLicense licenseType;

    @Column(nullable = false, length = 80)
    private String awsService;

    @Column(nullable = false, length = 180)
    private String saaDomain;

    @Column(nullable = false)
    private int estimatedMinutes;

    @Column(nullable = false, unique = true, length = 500)
    private String s3Key;

    @Column(nullable = false, length = 160)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private AppUser createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected StudyMaterial() {}

    public StudyMaterial(String title, String description, String author, String publisher, String edition,
                         Integer publicationYear, String isbn, String sourceUrl, MaterialType materialType,
                         MaterialLicense licenseType, String awsService, String saaDomain, int estimatedMinutes,
                         String s3Key, String contentType, long fileSize, AppUser createdBy) {
        this.title = title.trim();
        this.description = blankToNull(description);
        this.author = author.trim();
        this.publisher = blankToNull(publisher);
        this.edition = blankToNull(edition);
        this.publicationYear = publicationYear;
        this.isbn = blankToNull(isbn);
        this.sourceUrl = blankToNull(sourceUrl);
        this.materialType = materialType;
        this.licenseType = licenseType;
        this.awsService = awsService.trim();
        this.saaDomain = saaDomain.trim();
        this.estimatedMinutes = estimatedMinutes;
        this.s3Key = s3Key;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getAuthor() { return author; }
    public String getPublisher() { return publisher; }
    public String getEdition() { return edition; }
    public Integer getPublicationYear() { return publicationYear; }
    public String getIsbn() { return isbn; }
    public String getSourceUrl() { return sourceUrl; }
    public MaterialType getMaterialType() { return materialType; }
    public MaterialLicense getLicenseType() { return licenseType; }
    public String getAwsService() { return awsService; }
    public String getSaaDomain() { return saaDomain; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public String getS3Key() { return s3Key; }
    public String getContentType() { return contentType; }
    public long getFileSize() { return fileSize; }
    public boolean isActive() { return active; }
    public AppUser getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
