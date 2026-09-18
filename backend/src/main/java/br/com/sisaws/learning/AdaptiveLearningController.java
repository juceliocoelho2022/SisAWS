package br.com.sisaws.learning;

import br.com.sisaws.errornotebook.ErrorNotebookEntry;
import br.com.sisaws.errornotebook.ErrorNotebookRepository;
import br.com.sisaws.user.AppUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/learning/adaptive")
public class AdaptiveLearningController {

    private final ServiceProgressRepository progressRepository;
    private final ErrorNotebookRepository errorNotebookRepository;

    public AdaptiveLearningController(ServiceProgressRepository progressRepository,
                                      ErrorNotebookRepository errorNotebookRepository) {
        this.progressRepository = progressRepository;
        this.errorNotebookRepository = errorNotebookRepository;
    }

    @GetMapping("/plan")
    public AdaptivePlanResponse plan(Authentication authentication) {
        AppUser user = (AppUser) authentication.getPrincipal();

        List<ServiceProgress> progress = progressRepository.findAllByUserOrderByAwsServiceAsc(user);
        List<ErrorNotebookEntry> errors = errorNotebookRepository.findAllByUserOrderByLastWrongAtDesc(user);

        if (progress.isEmpty() && errors.isEmpty()) {
            return new AdaptivePlanResponse(
                    LocalDateTime.now().toString(),
                    "FOUNDATION",
                    List.of(new FocusServiceResponse(
                            "IAM",
                            0,
                            0,
                            0,
                            72,
                            "Comece pela base de identidade e acesso antes de avançar para arquiteturas mais complexas."
                    )),
                    new AdaptiveNextAction(
                            "IAM",
                            "EASY",
                            5,
                            "study",
                            "Construa sua linha de base com uma sessão curta em Modo Estudo."
                    )
            );
        }

        Map<String, Aggregate> aggregates = new HashMap<>();

        for (ServiceProgress item : progress) {
            aggregates.put(item.getAwsService().toUpperCase(Locale.ROOT), new Aggregate(
                    item.getAwsService(),
                    item.getAnswered(),
                    item.getCorrectAnswers(),
                    item.getAccuracyPercent(),
                    0
            ));
        }

        for (ErrorNotebookEntry entry : errors) {
            String service = entry.getQuestion().getAwsService();
            String key = service.toUpperCase(Locale.ROOT);
            Aggregate current = aggregates.getOrDefault(key, new Aggregate(service, 0, 0, 0, 0));
            aggregates.put(key, current.withWrongCount(current.wrongCount() + entry.getWrongCount()));
        }

        List<FocusServiceResponse> focus = aggregates.values().stream()
                .map(this::focus)
                .sorted(Comparator.comparingInt(FocusServiceResponse::priorityScore).reversed()
                        .thenComparing(FocusServiceResponse::awsService))
                .limit(5)
                .toList();

        FocusServiceResponse strongestNeed = focus.getFirst();
        String level = overallLevel(progress);
        String difficulty = suggestedDifficulty(strongestNeed.accuracyPercent(), strongestNeed.answered());
        int questionCount = strongestNeed.priorityScore() >= 70 ? 5 : 10;

        return new AdaptivePlanResponse(
                LocalDateTime.now().toString(),
                level,
                focus,
                new AdaptiveNextAction(
                        strongestNeed.awsService(),
                        difficulty,
                        questionCount,
                        "study",
                        nextActionReason(strongestNeed)
                )
        );
    }

    private FocusServiceResponse focus(Aggregate aggregate) {
        double accuracy = aggregate.answered() == 0 ? 0 : aggregate.accuracyPercent();
        double accuracyRisk = 100 - accuracy;
        double recurrenceRisk = Math.min(30, aggregate.wrongCount() * 5.0);
        double lowEvidenceRisk = aggregate.answered() < 3 ? 10 : 0;

        int priority = (int) Math.round(Math.min(
                100,
                accuracyRisk * 0.65 + recurrenceRisk + lowEvidenceRisk
        ));

        String reason;
        if (aggregate.wrongCount() >= 3) {
            reason = "Há reincidência de erros neste serviço; priorize revisão conceitual e prática curta.";
        } else if (aggregate.answered() < 3) {
            reason = "Ainda há poucos dados; responda mais questões para consolidar a análise.";
        } else if (accuracy < 60) {
            reason = "A taxa de acerto está abaixo de 60%, indicando necessidade de revisão.";
        } else if (accuracy < 80) {
            reason = "O desempenho é intermediário; prática direcionada pode consolidar o conteúdo.";
        } else {
            reason = "O desempenho está forte; mantenha prática espaçada para retenção.";
        }

        return new FocusServiceResponse(
                aggregate.awsService(),
                aggregate.answered(),
                aggregate.wrongCount(),
                accuracy,
                priority,
                reason
        );
    }

    private String overallLevel(List<ServiceProgress> progress) {
        if (progress.isEmpty()) return "FOUNDATION";

        double average = progress.stream()
                .mapToDouble(ServiceProgress::getAccuracyPercent)
                .average()
                .orElse(0);

        int answered = progress.stream()
                .mapToInt(ServiceProgress::getAnswered)
                .sum();

        if (answered < 10) return "FOUNDATION";
        if (average < 65) return "DEVELOPING";
        if (average < 80) return "CONSOLIDATING";
        return "STRONG";
    }

    private String suggestedDifficulty(double accuracy, int answered) {
        if (answered < 3 || accuracy < 55) return "EASY";
        if (accuracy < 75) return "MEDIUM";
        if (accuracy < 90) return "HARD";
        return "EXAM";
    }

    private String nextActionReason(FocusServiceResponse focus) {
        return "Prioridade " + focus.priorityScore() + "/100. "
                + focus.reason()
                + " O SisAWS recomenda começar por " + focus.awsService() + ".";
    }

    private record Aggregate(
            String awsService,
            int answered,
            int correctAnswers,
            double accuracyPercent,
            int wrongCount
    ) {
        Aggregate withWrongCount(int value) {
            return new Aggregate(awsService, answered, correctAnswers, accuracyPercent, value);
        }
    }

    public record FocusServiceResponse(
            String awsService,
            int answered,
            int wrongCount,
            double accuracyPercent,
            int priorityScore,
            String reason
    ) {}

    public record AdaptiveNextAction(
            String awsService,
            String difficulty,
            int questionCount,
            String mode,
            String reason
    ) {}

    public record AdaptivePlanResponse(
            String generatedAt,
            String overallLevel,
            List<FocusServiceResponse> focusServices,
            AdaptiveNextAction nextAction
    ) {}
}
