package br.com.sisaws.simulation;

import br.com.sisaws.errornotebook.ErrorNotebookEntry;
import br.com.sisaws.errornotebook.ErrorNotebookRepository;
import br.com.sisaws.learning.ServiceProgress;
import br.com.sisaws.learning.ServiceProgressRepository;
import br.com.sisaws.question.Question;
import br.com.sisaws.question.QuestionRepository;
import br.com.sisaws.user.AppUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/simulations")
public class SimulationController {
    private final QuestionRepository questionRepository;
    private final SimulationAttemptRepository attemptRepository;
    private final ErrorNotebookRepository errorNotebookRepository;
    private final ServiceProgressRepository serviceProgressRepository;

    public SimulationController(QuestionRepository questionRepository,
                                SimulationAttemptRepository attemptRepository,
                                ErrorNotebookRepository errorNotebookRepository,
                                ServiceProgressRepository serviceProgressRepository) {
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.errorNotebookRepository = errorNotebookRepository;
        this.serviceProgressRepository = serviceProgressRepository;
    }

    @PostMapping("/finish")
    @Transactional
    public ResponseEntity<SimulationResult> finish(@Valid @RequestBody FinishSimulationRequest request,
                                                   Authentication authentication) {
        AppUser user = (AppUser) authentication.getPrincipal();
        int correct = 0;
        List<QuestionResult> results = new ArrayList<>();

        for (AnswerRequest answer : request.answers()) {
            Optional<Question> optional = questionRepository.findById(answer.questionId());
            if (optional.isEmpty()) continue;

            Question question = optional.get();
            Set<Long> expected = new HashSet<>(question.getOptions().stream()
                    .filter(option -> option.isCorrect())
                    .map(option -> option.getId())
                    .toList());
            Set<Long> selected = answer.selectedOptionIds() == null
                    ? Set.of()
                    : new HashSet<>(answer.selectedOptionIds());
            boolean isCorrect = expected.equals(selected);

            registerServiceProgress(user, question, isCorrect);

            if (isCorrect) {
                correct++;
            } else {
                registerError(user, question);
            }

            results.add(new QuestionResult(
                    question.getId(),
                    isCorrect,
                    question.getExplanation(),
                    question.getOptions().stream()
                            .filter(option -> option.isCorrect())
                            .map(option -> option.getId())
                            .toList()
            ));
        }

        int total = results.size();
        double score = total == 0 ? 0 : Math.round((correct * 10000.0) / total) / 100.0;

        SimulationAttempt saved = attemptRepository.save(
                new SimulationAttempt(user, request.certificationCode(), total, correct, score)
        );

        return ResponseEntity.ok(new SimulationResult(saved.getId(), total, correct, score, results));
    }

    @GetMapping("/history")
    public List<HistoryResponse> history(Authentication authentication) {
        AppUser user = (AppUser) authentication.getPrincipal();

        return attemptRepository.findTop10ByUserOrderByFinishedAtDesc(user).stream()
                .map(attempt -> new HistoryResponse(
                        attempt.getId(),
                        attempt.getCertificationCode(),
                        attempt.getTotalQuestions(),
                        attempt.getCorrectAnswers(),
                        attempt.getScorePercent(),
                        attempt.getFinishedAt().toString()
                ))
                .toList();
    }

    private void registerServiceProgress(AppUser user, Question question, boolean correct) {
        ServiceProgress progress = serviceProgressRepository
                .findByUserAndAwsServiceIgnoreCase(user, question.getAwsService())
                .orElseGet(() -> new ServiceProgress(user, question.getAwsService()));

        progress.recordAnswer(correct);
        serviceProgressRepository.save(progress);
    }

    private void registerError(AppUser user, Question question) {
        ErrorNotebookEntry entry = errorNotebookRepository.findByUserAndQuestion(user, question)
                .orElseGet(() -> new ErrorNotebookEntry(user, question));

        if (entry.getId() != null) {
            entry.recordWrongAnswer();
        }

        errorNotebookRepository.save(entry);
    }

    public record AnswerRequest(Long questionId, List<Long> selectedOptionIds) {}

    public record FinishSimulationRequest(
            @NotBlank String certificationCode,
            @NotEmpty List<AnswerRequest> answers
    ) {}

    public record QuestionResult(
            Long questionId,
            boolean correct,
            String explanation,
            List<Long> correctOptionIds
    ) {}

    public record SimulationResult(
            Long attemptId,
            int totalQuestions,
            int correctAnswers,
            double scorePercent,
            List<QuestionResult> results
    ) {}

    public record HistoryResponse(
            Long id,
            String certificationCode,
            int totalQuestions,
            int correctAnswers,
            double scorePercent,
            String finishedAt
    ) {}
}
