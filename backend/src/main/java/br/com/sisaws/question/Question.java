package br.com.sisaws.question;

import br.com.sisaws.certification.Certification;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Certification certification;

    @Column(nullable = false, length = 80)
    private String domain;

    @Column(nullable = false, length = 80)
    private String awsService;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    @Column(nullable = false, length = 3000)
    private String prompt;

    @Column(nullable = false, length = 3000)
    private String explanation;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<AnswerOption> options = new ArrayList<>();

    protected Question() {}

    public Question(Certification certification, String domain, String awsService,
                    Difficulty difficulty, String prompt, String explanation) {
        this.certification = certification;
        this.domain = domain;
        this.awsService = awsService;
        this.difficulty = difficulty;
        this.prompt = prompt;
        this.explanation = explanation;
    }

    public void addOption(String text, boolean correct) {
        options.add(new AnswerOption(this, text, correct));
    }

    public Long getId() { return id; }
    public Certification getCertification() { return certification; }
    public String getDomain() { return domain; }
    public String getAwsService() { return awsService; }
    public Difficulty getDifficulty() { return difficulty; }
    public String getPrompt() { return prompt; }
    public String getExplanation() { return explanation; }
    public List<AnswerOption> getOptions() { return options; }
}
