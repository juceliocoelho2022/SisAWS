package br.com.sisaws.learning;

import br.com.sisaws.user.AppUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/learning")
public class LearningController {

    private final ServiceProgressRepository progressRepository;

    public LearningController(ServiceProgressRepository progressRepository) {
        this.progressRepository = progressRepository;
    }

    @GetMapping("/progress")
    public List<ServiceProgressResponse> progress(Authentication authentication) {
        AppUser user = (AppUser) authentication.getPrincipal();

        return progressRepository.findAllByUserOrderByAwsServiceAsc(user).stream()
                .map(progress -> new ServiceProgressResponse(
                        progress.getAwsService(),
                        progress.getAnswered(),
                        progress.getCorrectAnswers(),
                        progress.getAccuracyPercent(),
                        status(progress.getAccuracyPercent(), progress.getAnswered()),
                        progress.getLastAnsweredAt().toString()
                ))
                .toList();
    }

    @GetMapping("/recommendation")
    public RecommendationResponse recommendation(Authentication authentication) {
        AppUser user = (AppUser) authentication.getPrincipal();
        var progress = progressRepository.findAllByUserOrderByAwsServiceAsc(user);

        if (progress.isEmpty()) {
            return new RecommendationResponse(
                    "IAM",
                    "Comece por identidade e acesso",
                    "Você ainda não possui respostas suficientes para gerar uma recomendação personalizada.",
                    "Estude IAM e depois faça um simulado para iniciar suas métricas.",
                    "FOUNDATION"
            );
        }

        ServiceProgress weakest = progress.stream()
                .min(Comparator
                        .comparingDouble(ServiceProgress::getAccuracyPercent)
                        .thenComparing(Comparator.comparingInt(ServiceProgress::getAnswered).reversed()))
                .orElse(progress.getFirst());

        double accuracy = weakest.getAccuracyPercent();
        String reason = "Seu desempenho em " + weakest.getAwsService() + " está em " + accuracy
                + "% após " + weakest.getAnswered() + " resposta(s).";

        return new RecommendationResponse(
                weakest.getAwsService(),
                "Priorize " + weakest.getAwsService(),
                reason,
                accuracy < 70
                        ? "Revise os flashcards do serviço e faça um simulado focado."
                        : "Continue praticando para consolidar o conhecimento.",
                accuracy < 70 ? "REVIEW" : "PRACTICE"
        );
    }

    @GetMapping("/trails")
    public List<StudyTrailResponse> trails() {
        return TRAILS;
    }

    @GetMapping("/flashcards")
    public List<FlashcardResponse> flashcards(@RequestParam(required = false) String service) {
        if (service == null || service.isBlank()) {
            return FLASHCARDS;
        }

        return FLASHCARDS.stream()
                .filter(card -> card.awsService().equalsIgnoreCase(service))
                .toList();
    }

    private String status(double accuracy, int answered) {
        if (answered < 3) return "STARTING";
        if (accuracy < 60) return "REVIEW";
        if (accuracy < 80) return "GOOD";
        return "STRONG";
    }

    private static final List<StudyTrailResponse> TRAILS = List.of(
            new StudyTrailResponse(
                    "security-foundations",
                    "Identidade e Segurança",
                    "Construa a base de segurança antes de avançar para arquiteturas mais complexas.",
                    List.of("IAM", "KMS", "VPC"),
                    "FOUNDATION",
                    90
            ),
            new StudyTrailResponse(
                    "compute-scaling",
                    "Compute e Escalabilidade",
                    "Entenda computação elástica, balanceamento e crescimento automático de capacidade.",
                    List.of("EC2", "Auto Scaling"),
                    "INTERMEDIATE",
                    100
            ),
            new StudyTrailResponse(
                    "storage-delivery",
                    "Storage e Entrega de Conteúdo",
                    "Pratique armazenamento de objetos, ciclo de vida e distribuição global de conteúdo.",
                    List.of("S3", "CloudFront"),
                    "INTERMEDIATE",
                    90
            ),
            new StudyTrailResponse(
                    "data-services",
                    "Bancos de Dados",
                    "Compare bancos relacionais e NoSQL para selecionar a solução adequada a cada cenário.",
                    List.of("RDS", "DynamoDB"),
                    "INTERMEDIATE",
                    100
            ),
            new StudyTrailResponse(
                    "resilience-integration",
                    "Resiliência e Integração",
                    "Desacople componentes, absorva picos e projete aplicações tolerantes a falhas.",
                    List.of("SQS", "Route 53"),
                    "ADVANCED",
                    110
            )
    );

    private static final List<FlashcardResponse> FLASHCARDS = List.of(
            new FlashcardResponse(1L, "IAM", "Para que serve uma IAM Role?", "Conceder permissões temporárias a identidades ou recursos sem depender de credenciais estáticas."),
            new FlashcardResponse(2L, "IAM", "Qual princípio deve orientar permissões?", "Princípio do menor privilégio: conceder apenas as permissões necessárias para a tarefa."),
            new FlashcardResponse(3L, "VPC", "Security Group é stateful ou stateless?", "Stateful. O tráfego de resposta é permitido de acordo com o estado da conexão."),
            new FlashcardResponse(4L, "S3", "O que uma Lifecycle Rule automatiza?", "Transições entre classes de armazenamento e expiração de objetos conforme regras definidas."),
            new FlashcardResponse(5L, "CloudFront", "Qual o papel principal do CloudFront?", "Distribuir conteúdo por edge locations para reduzir latência e aliviar a origem."),
            new FlashcardResponse(6L, "EC2", "Quando Spot Instances fazem sentido?", "Em workloads tolerantes a interrupções que podem aproveitar capacidade ociosa com menor custo."),
            new FlashcardResponse(7L, "Auto Scaling", "O que um Auto Scaling Group controla?", "A quantidade de instâncias EC2 conforme capacidade mínima, desejada, máxima e políticas de escala."),
            new FlashcardResponse(8L, "RDS", "Qual objetivo principal de Multi-AZ?", "Aumentar disponibilidade com réplica sincronizada e failover gerenciado."),
            new FlashcardResponse(9L, "DynamoDB", "Que tipo de banco é o DynamoDB?", "Banco NoSQL totalmente gerenciado, orientado a chave-valor e documentos, com baixa latência em escala."),
            new FlashcardResponse(10L, "SQS", "Por que colocar SQS entre serviços?", "Para desacoplar produtor e consumidor, absorver picos e permitir processamento assíncrono."),
            new FlashcardResponse(11L, "KMS", "O que o AWS KMS gerencia?", "Chaves criptográficas e políticas de uso integradas a diversos serviços AWS."),
            new FlashcardResponse(12L, "Route 53", "Para que servem Health Checks no Route 53?", "Para avaliar a saúde de endpoints e apoiar decisões de roteamento DNS.")
    );

    public record ServiceProgressResponse(
            String awsService,
            int answered,
            int correctAnswers,
            double accuracyPercent,
            String status,
            String lastAnsweredAt
    ) {}

    public record RecommendationResponse(
            String awsService,
            String title,
            String reason,
            String recommendedAction,
            String priority
    ) {}

    public record StudyTrailResponse(
            String id,
            String title,
            String description,
            List<String> services,
            String level,
            int estimatedMinutes
    ) {}

    public record FlashcardResponse(
            Long id,
            String awsService,
            String question,
            String answer
    ) {}
}
