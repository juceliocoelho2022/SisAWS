package br.com.sisaws.simulation;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "simulation_attempts")
public class SimulationAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String certificationCode;

    @Column(nullable = false)
    private int totalQuestions;

    @Column(nullable = false)
    private int correctAnswers;

    @Column(nullable = false)
    private double scorePercent;

    @Column(nullable = false)
    private LocalDateTime finishedAt;

    protected SimulationAttempt() {}

    public SimulationAttempt(String certificationCode, int totalQuestions, int correctAnswers, double scorePercent) {
        this.certificationCode = certificationCode;
        this.totalQuestions = totalQuestions;
        this.correctAnswers = correctAnswers;
        this.scorePercent = scorePercent;
        this.finishedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getCertificationCode() { return certificationCode; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getCorrectAnswers() { return correctAnswers; }
    public double getScorePercent() { return scorePercent; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
}
