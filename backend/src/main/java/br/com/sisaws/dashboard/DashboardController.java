package br.com.sisaws.dashboard;

import br.com.sisaws.errornotebook.ErrorNotebookRepository;
import br.com.sisaws.question.QuestionRepository;
import br.com.sisaws.simulation.SimulationAttemptRepository;
import br.com.sisaws.user.AppUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final QuestionRepository questionRepository;
    private final SimulationAttemptRepository attemptRepository;
    private final ErrorNotebookRepository errorNotebookRepository;

    public DashboardController(QuestionRepository questionRepository,
                               SimulationAttemptRepository attemptRepository,
                               ErrorNotebookRepository errorNotebookRepository) {
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.errorNotebookRepository = errorNotebookRepository;
    }

    @GetMapping
    public DashboardResponse dashboard(Authentication authentication) {
        AppUser user = (AppUser) authentication.getPrincipal();
        var attempts = attemptRepository.findAllByUser(user);

        double average = attempts.stream()
                .mapToDouble(a -> a.getScorePercent())
                .average()
                .orElse(0);

        double best = attempts.stream()
                .mapToDouble(a -> a.getScorePercent())
                .max()
                .orElse(0);

        return new DashboardResponse(
                questionRepository.countByCertification_CodeIgnoreCase("SAA-C03"),
                attempts.size(),
                Math.round(average * 100.0) / 100.0,
                Math.round(best * 100.0) / 100.0,
                errorNotebookRepository.countByUser(user)
        );
    }

    public record DashboardResponse(
            long questionBank,
            long attempts,
            double averageScore,
            double bestScore,
            long errorNotebookCount
    ) {}
}
