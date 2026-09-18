package br.com.sisaws.certification;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/certifications")
@CrossOrigin(origins = "http://localhost:5173")
public class CertificationController {
    private final CertificationRepository repository;

    public CertificationController(CertificationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<CertificationResponse> list() {
        return repository.findAll().stream()
                .map(c -> new CertificationResponse(c.getCode(), c.getName()))
                .toList();
    }

    public record CertificationResponse(String code, String name) {}
}
