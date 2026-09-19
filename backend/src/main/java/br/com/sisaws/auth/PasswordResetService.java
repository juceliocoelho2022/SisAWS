package br.com.sisaws.auth;

import br.com.sisaws.user.AppUser;
import br.com.sisaws.user.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppUserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetNotifier notifier;
    private final long ttlMinutes;

    public PasswordResetService(
            AppUserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetNotifier notifier,
            @Value("${sisaws.security.password-reset.ttl-minutes:15}") long ttlMinutes) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.notifier = notifier;
        this.ttlMinutes = ttlMinutes;
    }

    @Transactional
    public void requestReset(String email) {
        String normalizedEmail = AppUser.normalizeEmail(email);

        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(user -> {
            tokenRepository.deleteAllByUser(user);

            String rawToken = generateToken();
            tokenRepository.save(new PasswordResetToken(
                    user,
                    hashToken(rawToken),
                    LocalDateTime.now().plusMinutes(ttlMinutes)
            ));

            notifier.sendResetLink(user, rawToken);
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(hashToken(rawToken))
                .orElseThrow(PasswordResetService::invalidToken);

        if (token.isExpired()) {
            tokenRepository.delete(token);
            throw invalidToken();
        }

        AppUser user = token.getUser();
        user.changePassword(passwordEncoder.encode(newPassword));
        tokenRepository.deleteAllByUser(user);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static ResponseStatusException invalidToken() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link de redefinição inválido ou expirado");
    }
}
