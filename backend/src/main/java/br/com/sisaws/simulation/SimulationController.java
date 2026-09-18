package br.com.sisaws.simulation;

import br.com.sisaws.question.Question;
import br.com.sisaws.question.QuestionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/simulations")
@CrossOrigin(origins = "http://localhost:5173")
public class SimulationController {
    private final QuestionRepository questionRepository;
    private final SimulationAttemptRepository attemptRepository;

    public SimulationController(QuestionRepository questionRepository,
                                SimulationAttemptRepository attemptRepository) {
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
    }

    @PostMapping("/finish")
    public ResponseEntity<SimulationResult> finish(@Valid @RequestBody FinishSimulationRequest request) {
        int correct = 0;
        List<QuestionResult> results = new ArrayList<>();

        for (AnswerRequest answer : request.answers()) {
            Optional<Question> optional = questionRepository.findById(answer.questionId());
            if (optional.isEmpty()) continue;

            Question question = optional.get();
            Set<Long> expected = new HashSet<>(question.getOptions().stream()
                    .filter(o -> o.isCorrect())
                    .map(o -> o.getId())
                    .toList());
            Set<Long> selected = new HashSet<>(answer.selectedOptionIds());
            boolean isCorrect = expected.equals(selected);
            if (isCorrect) correct++;

            results.add(new QuestionResult(
                    question.getId(),
                    isCorrect,
                    question.getExplanation(),
                    question.getOptions().stream().filter(o -> o.isCorrect()).map(o -> o.getId()).toList()
            ));
        }

        int total = results.size();
        double score = total == 0 ? 0 : Math.round((correct * 10000.0) / total) / 100.0;
        SimulationAttempt saved = attemptRepository.save(
                new SimulationAttempt(request.certificationCode(), total, correct, score)
        );

        return ResponseEntity.ok(new SimulationResult(saved.getId(), total, correct, score, results));
    }

    @GetMapping("/history")
    public List<HistoryResponse> history() {
        return attemptRepository.findTop10ByOrderByFinishedAtDesc().stream()
                .map(a -> new HistoryResponse(a.getId(), a.getCertificationCode(), a.getTotalQuestions(),
                        a.getCorrectAnswers(), a.getScorePercent(), a.getFinishedAt().toString()))
                .toList();
    }

    public record AnswerRequest(Long questionId, @NotEmpty List<Long> selectedOptionIds) {}
    public record FinishSimulationRequest(@NotBlank String certificationCode,
                                          @NotEmpty List<AnswerRequest> answers) {}
    public record QuestionResult(Long questionId, boolean correct, String explanation,
                                 List<Long> correctOptionIds) {}
    public record SimulationResult(Long attemptId, int totalQuestions, int correctAnswers,
                                   double scorePercent, List<QuestionResult> results) {}
    public record HistoryResponse(Long id, String certificationCode, int totalQuestions,
                                  int correctAnswers, double scorePercent, String finishedAt) {}
}
