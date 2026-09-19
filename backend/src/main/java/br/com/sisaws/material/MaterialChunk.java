package br.com.sisaws.material;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "material_chunks",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_material_chunk_index",
                columnNames = {"material_id", "chunk_index"}
        )
)
public class MaterialChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private StudyMaterial material;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected MaterialChunk() {}

    public MaterialChunk(StudyMaterial material, int chunkIndex, String content) {
        this.material = material;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public StudyMaterial getMaterial() { return material; }
    public int getChunkIndex() { return chunkIndex; }
    public String getContent() { return content; }
}
