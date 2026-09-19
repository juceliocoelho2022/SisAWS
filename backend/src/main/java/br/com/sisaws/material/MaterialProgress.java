package br.com.sisaws.material;

import br.com.sisaws.user.AppUser;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_material_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_material_progress_user_chapter",
                columnNames = {"user_id", "chapter_id"}
        )
)
public class MaterialProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    private StudyChapter chapter;

    @Column(nullable = false)
    private int progressPercent;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected MaterialProgress() {}

    public MaterialProgress(AppUser user, StudyChapter chapter) {
        this.user = user;
        this.chapter = chapter;
        this.progressPercent = 0;
        this.startedAt = LocalDateTime.now();
        this.updatedAt = this.startedAt;
    }

    public void complete() {
        this.progressPercent = 100;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = this.completedAt;
    }

    public StudyChapter getChapter() { return chapter; }
    public int getProgressPercent() { return progressPercent; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
