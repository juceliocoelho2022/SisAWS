package br.com.sisaws.auth;

import br.com.sisaws.user.AppUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return response(authService.register(request.name(), request.email(), request.password()));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return response(authService.login(request.email(), request.password()));
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return user((AppUser) authentication.getPrincipal());
    }

    private AuthResponse response(AuthService.AuthResult result) {
        return new AuthResponse(result.token(), user(result.user()));
    }

    private UserResponse user(AppUser user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 120) String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record UserResponse(Long id, String name, String email, String role) {}
    public record AuthResponse(String token, UserResponse user) {}
}
