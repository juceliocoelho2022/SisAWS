package br.com.sisaws.errornotebook;

import br.com.sisaws.question.Question;
import br.com.sisaws.user.AppUser;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "error_notebook_entries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_error_notebook_user_question",
                columnNames = {"user_id", "question_id"}
        )
)
public class ErrorNotebookEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private int wrongCount;

    @Column(nullable = false)
    private LocalDateTime lastWrongAt;

    protected ErrorNotebookEntry() {}

    public ErrorNotebookEntry(AppUser user, Question question) {
        this.user = user;
        this.question = question;
        this.wrongCount = 1;
        this.lastWrongAt = LocalDateTime.now();
    }

    public void recordWrongAnswer() {
        this.wrongCount++;
        this.lastWrongAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public Question getQuestion() { return question; }
    public int getWrongCount() { return wrongCount; }
    public LocalDateTime getLastWrongAt() { return lastWrongAt; }
}
