package br.com.sisaws.auth;

import br.com.sisaws.security.JwtService;
import br.com.sisaws.user.AppUser;
import br.com.sisaws.user.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${sisaws.instructor.emails:}")
    private String instructorEmails = "";

    public AuthService(AppUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResult register(String name, String email, String password) {
        String normalizedEmail = AppUser.normalizeEmail(email);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuário com este e-mail");
        }

        AppUser user = new AppUser(name, normalizedEmail, passwordEncoder.encode(password));
        applyConfiguredRole(user);
        user = userRepository.save(user);
        return result(user);
    }

    public AuthResult login(String email, String password) {
        AppUser user = userRepository.findByEmailIgnoreCase(AppUser.normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");
        }

        if (applyConfiguredRole(user)) {
            userRepository.save(user);
        }

        return result(user);
    }

    private boolean applyConfiguredRole(AppUser user) {
        if (user.getRole() == br.com.sisaws.user.Role.INSTRUCTOR || instructorEmails == null || instructorEmails.isBlank()) {
            return false;
        }

        boolean configured = java.util.Arrays.stream(instructorEmails.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(AppUser::normalizeEmail)
                .anyMatch(user.getEmail()::equals);

        if (configured) {
            user.promoteToInstructor();
            return true;
        }

        return false;
    }

    private AuthResult result(AppUser user) {
        return new AuthResult(jwtService.generateToken(user), user);
    }

    public record AuthResult(String token, AppUser user) {}
}
