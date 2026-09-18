package br.com.sisaws.dashboard;

import br.com.sisaws.question.QuestionRepository;
import br.com.sisaws.simulation.SimulationAttemptRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@CrossOrigin(origins = "http://localhost:5173")
public class DashboardController {
    private final QuestionRepository questionRepository;
    private final SimulationAttemptRepository attemptRepository;

    public DashboardController(QuestionRepository questionRepository,
                               SimulationAttemptRepository attemptRepository) {
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
    }

    @GetMapping
    public DashboardResponse dashboard() {
        var attempts = attemptRepository.findAll();
        double average = attempts.stream().mapToDouble(a -> a.getScorePercent()).average().orElse(0);
        double best = attempts.stream().mapToDouble(a -> a.getScorePercent()).max().orElse(0);

        return new DashboardResponse(
                questionRepository.countByCertification_CodeIgnoreCase("SAA-C03"),
                attempts.size(),
                Math.round(average * 100.0) / 100.0,
                Math.round(best * 100.0) / 100.0
        );
    }

    public record DashboardResponse(long questionBank, long attempts, double averageScore, double bestScore) {}
}
