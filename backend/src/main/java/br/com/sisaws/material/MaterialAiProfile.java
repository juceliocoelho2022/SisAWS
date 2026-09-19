package br.com.sisaws.material;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "material_ai_profiles")
public class MaterialAiProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false, unique = true)
    private StudyMaterial material;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String summary;

    @Lob
    @Column(name = "key_points", columnDefinition = "TEXT")
    private String keyPoints;

    @Column(nullable = false, length = 30)
    private String generationMode;

    @Column(nullable = false)
    private int chunkCount;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    protected MaterialAiProfile() {}

    public MaterialAiProfile(StudyMaterial material, String summary, String keyPoints, String generationMode, int chunkCount) {
        this.material = material;
        this.summary = summary;
        this.keyPoints = keyPoints;
        this.generationMode = generationMode;
        this.chunkCount = chunkCount;
        this.processedAt = LocalDateTime.now();
    }

    public StudyMaterial getMaterial() { return material; }
    public String getSummary() { return summary; }
    public String getKeyPoints() { return keyPoints; }
    public String getGenerationMode() { return generationMode; }
    public int getChunkCount() { return chunkCount; }
    public LocalDateTime getProcessedAt() { return processedAt; }
}
