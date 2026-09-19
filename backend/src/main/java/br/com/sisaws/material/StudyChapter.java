package br.com.sisaws.material;

import jakarta.persistence.*;

@Entity
@Table(
        name = "study_chapters",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_study_chapter_material_number",
                columnNames = {"material_id", "chapter_number"}
        )
)
public class StudyChapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private StudyMaterial material;

    @Column(name = "chapter_number", nullable = false)
    private int chapterNumber;

    @Column(nullable = false, length = 220)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 80)
    private String awsService;

    @Column(nullable = false, length = 180)
    private String saaDomain;

    @Column(nullable = false)
    private int estimatedMinutes;

    protected StudyChapter() {}

    public StudyChapter(StudyMaterial material, int chapterNumber, String title, String description,
                        String awsService, String saaDomain, int estimatedMinutes) {
        this.material = material;
        this.chapterNumber = chapterNumber;
        this.title = title.trim();
        this.description = description == null || description.isBlank() ? null : description.trim();
        this.awsService = awsService.trim();
        this.saaDomain = saaDomain.trim();
        this.estimatedMinutes = estimatedMinutes;
    }

    public Long getId() { return id; }
    public StudyMaterial getMaterial() { return material; }
    public int getChapterNumber() { return chapterNumber; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getAwsService() { return awsService; }
    public String getSaaDomain() { return saaDomain; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
}
