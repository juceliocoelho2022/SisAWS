package br.com.sisaws.errornotebook;

import br.com.sisaws.user.AppUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/error-notebook")
public class ErrorNotebookController {

    private final ErrorNotebookRepository repository;

    public ErrorNotebookController(ErrorNotebookRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ErrorEntryResponse> list(Authentication authentication) {
        AppUser user = (AppUser) authentication.getPrincipal();

        return repository.findAllByUserOrderByLastWrongAtDesc(user).stream()
                .map(entry -> new ErrorEntryResponse(
                        entry.getId(),
                        entry.getQuestion().getId(),
                        entry.getQuestion().getAwsService(),
                        entry.getQuestion().getDomain(),
                        entry.getQuestion().getPrompt(),
                        entry.getQuestion().getExplanation(),
                        entry.getWrongCount(),
                        entry.getLastWrongAt().toString()
                ))
                .toList();
    }

    public record ErrorEntryResponse(
            Long id,
            Long questionId,
            String awsService,
            String domain,
            String prompt,
            String explanation,
            int wrongCount,
            String lastWrongAt
    ) {}
}
