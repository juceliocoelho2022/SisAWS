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
    private final PasswordResetService passwordResetService;
    private final AuthRateLimiter rateLimiter;

    public AuthController(AuthService authService,
                          PasswordResetService passwordResetService,
                          AuthRateLimiter rateLimiter) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request,
                                 jakarta.servlet.http.HttpServletRequest httpRequest) {
        rateLimiter.checkRegister(httpRequest);
        return response(authService.register(request.name(), request.email(), request.password()));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request,
                              jakarta.servlet.http.HttpServletRequest httpRequest) {
        rateLimiter.checkLogin(httpRequest, request.email());
        return response(authService.login(request.email(), request.password()));
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(org.springframework.http.HttpStatus.ACCEPTED)
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                          jakarta.servlet.http.HttpServletRequest httpRequest) {
        rateLimiter.checkForgotPassword(httpRequest, request.email());
        passwordResetService.requestReset(request.email());
        return new MessageResponse(
                "Se existir uma conta com este e-mail, enviaremos um link temporário para redefinir a senha."
        );
    }

    @PostMapping("/reset-password")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                              jakarta.servlet.http.HttpServletRequest httpRequest) {
        rateLimiter.checkResetPassword(httpRequest);
        passwordResetService.resetPassword(request.token(), request.newPassword());
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

    public record ForgotPasswordRequest(
            @NotBlank @Email String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 72) String newPassword
    ) {}

    public record MessageResponse(String message) {}

    public record UserResponse(Long id, String name, String email, String role) {}
    public record AuthResponse(String token, UserResponse user) {}
}
