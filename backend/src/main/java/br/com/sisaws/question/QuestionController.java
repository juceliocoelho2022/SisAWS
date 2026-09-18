package br.com.sisaws.question;

import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/questions")
public class QuestionController {
    private final QuestionRepository repository;

    public QuestionController(QuestionRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<QuestionResponse> list(
            @RequestParam(defaultValue = "SAA-C03") String certification,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) Difficulty difficulty) {

        List<Question> questions = repository.findByCertification_CodeIgnoreCase(certification).stream()
                .filter(question -> service == null || service.isBlank()
                        || question.getAwsService().equalsIgnoreCase(service))
                .filter(question -> difficulty == null || question.getDifficulty() == difficulty)
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));

        Collections.shuffle(questions);

        return questions.stream()
                .limit(Math.max(1, Math.min(limit, 65)))
                .map(QuestionResponse::from)
                .toList();
    }

    @PostMapping("/{id}/check")
    public AnswerCheckResponse check(@PathVariable Long id,
                                     @RequestBody AnswerCheckRequest request) {
        Question question = repository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Questão não encontrada"
                ));

        Set<Long> expected = new HashSet<>(question.getOptions().stream()
                .filter(AnswerOption::isCorrect)
                .map(AnswerOption::getId)
                .toList());

        Set<Long> selected = request.selectedOptionIds() == null
                ? Set.of()
                : new HashSet<>(request.selectedOptionIds());

        return new AnswerCheckResponse(
                expected.equals(selected),
                question.getExplanation(),
                expected.stream().toList()
        );
    }

    public record AnswerCheckRequest(List<Long> selectedOptionIds) {}

    public record AnswerCheckResponse(
            boolean correct,
            String explanation,
            List<Long> correctOptionIds
    ) {}

    public record OptionResponse(Long id, String text) {}

    public record QuestionResponse(
            Long id,
            String domain,
            String awsService,
            Difficulty difficulty,
            String prompt,
            List<OptionResponse> options) {

        static QuestionResponse from(Question q) {
            return new QuestionResponse(
                    q.getId(),
                    q.getDomain(),
                    q.getAwsService(),
                    q.getDifficulty(),
                    q.getPrompt(),
                    q.getOptions().stream()
                            .map(option -> new OptionResponse(option.getId(), option.getText()))
                            .toList()
            );
        }
    }
}
