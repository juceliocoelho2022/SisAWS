package br.com.sisaws.learning;

import br.com.sisaws.user.AppUser;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "service_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_service_progress_user_service",
                columnNames = {"user_id", "aws_service"}
        )
)
public class ServiceProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "aws_service", nullable = false, length = 80)
    private String awsService;

    @Column(nullable = false)
    private int answered;

    @Column(nullable = false)
    private int correctAnswers;

    @Column(nullable = false)
    private LocalDateTime lastAnsweredAt;

    protected ServiceProgress() {}

    public ServiceProgress(AppUser user, String awsService) {
        this.user = user;
        this.awsService = awsService;
        this.answered = 0;
        this.correctAnswers = 0;
        this.lastAnsweredAt = LocalDateTime.now();
    }

    public void recordAnswer(boolean correct) {
        answered++;
        if (correct) {
            correctAnswers++;
        }
        lastAnsweredAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public String getAwsService() { return awsService; }
    public int getAnswered() { return answered; }
    public int getCorrectAnswers() { return correctAnswers; }
    public LocalDateTime getLastAnsweredAt() { return lastAnsweredAt; }

    public double getAccuracyPercent() {
        if (answered == 0) return 0;
        return Math.round((correctAnswers * 10000.0) / answered) / 100.0;
    }
}
