package br.com.sisaws.material;

import jakarta.persistence.*;

@Entity
@Table(name = "material_ai_flashcards")
public class MaterialAiFlashcard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private StudyMaterial material;

    @Column(nullable = false, length = 500)
    private String question;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    protected MaterialAiFlashcard() {}

    public MaterialAiFlashcard(StudyMaterial material, String question, String answer) {
        this.material = material;
        this.question = question;
        this.answer = answer;
    }

    public Long getId() { return id; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
}
