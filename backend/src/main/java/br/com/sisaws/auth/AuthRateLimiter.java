package br.com.sisaws.auth;

import br.com.sisaws.user.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class AuthRateLimiter {

    private static final long LOGIN_IP_WINDOW_SECONDS = 60;
    private static final long LOGIN_EMAIL_WINDOW_SECONDS = 300;
    private static final long FORGOT_WINDOW_SECONDS = 900;
    private static final long RESET_WINDOW_SECONDS = 300;
    private static final long REGISTER_WINDOW_SECONDS = 900;
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    private final int loginIpLimit;
    private final int loginEmailLimit;
    private final int forgotIpLimit;
    private final int forgotEmailLimit;
    private final int resetIpLimit;
    private final int registerIpLimit;

    public AuthRateLimiter(
            @Value("${sisaws.security.rate-limit.login-ip-limit:10}") int loginIpLimit,
            @Value("${sisaws.security.rate-limit.login-email-limit:5}") int loginEmailLimit,
            @Value("${sisaws.security.rate-limit.forgot-ip-limit:5}") int forgotIpLimit,
            @Value("${sisaws.security.rate-limit.forgot-email-limit:3}") int forgotEmailLimit,
            @Value("${sisaws.security.rate-limit.reset-ip-limit:5}") int resetIpLimit,
            @Value("${sisaws.security.rate-limit.register-ip-limit:5}") int registerIpLimit) {
        this.loginIpLimit = loginIpLimit;
        this.loginEmailLimit = loginEmailLimit;
        this.forgotIpLimit = forgotIpLimit;
        this.forgotEmailLimit = forgotEmailLimit;
        this.resetIpLimit = resetIpLimit;
        this.registerIpLimit = registerIpLimit;
    }

    public void checkLogin(HttpServletRequest request, String email) {
        String ip = clientIp(request);
        consume("login:ip:" + ip, loginIpLimit, LOGIN_IP_WINDOW_SECONDS);
        consume("login:email:" + hashEmail(email), loginEmailLimit, LOGIN_EMAIL_WINDOW_SECONDS);
    }

    public void checkForgotPassword(HttpServletRequest request, String email) {
        String ip = clientIp(request);
        consume("forgot:ip:" + ip, forgotIpLimit, FORGOT_WINDOW_SECONDS);
        consume("forgot:email:" + hashEmail(email), forgotEmailLimit, FORGOT_WINDOW_SECONDS);
    }

    public void checkResetPassword(HttpServletRequest request) {
        consume("reset:ip:" + clientIp(request), resetIpLimit, RESET_WINDOW_SECONDS);
    }

    public void checkRegister(HttpServletRequest request) {
        consume("register:ip:" + clientIp(request), registerIpLimit, REGISTER_WINDOW_SECONDS);
    }

    private void consume(String key, int limit, long windowSeconds) {
        long now = Instant.now().getEpochSecond();
        AtomicReference<Decision> decision = new AtomicReference<>();

        counters.compute(key, (ignored, current) -> {
            if (current == null || now >= current.resetAtEpochSecond()) {
                decision.set(new Decision(true, windowSeconds));
                return new WindowCounter(1, now + windowSeconds);
            }

            if (current.count() >= limit) {
                long retryAfter = Math.max(1, current.resetAtEpochSecond() - now);
                decision.set(new Decision(false, retryAfter));
                return current;
            }

            decision.set(new Decision(true, current.resetAtEpochSecond() - now));
            return new WindowCounter(current.count() + 1, current.resetAtEpochSecond());
        });

        if (counters.size() > CLEANUP_THRESHOLD) {
            counters.entrySet().removeIf(entry -> entry.getValue().resetAtEpochSecond() <= now);
        }

        if (!decision.get().allowed()) {
            throw new RateLimitExceededException(decision.get().retryAfterSeconds());
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            String[] addresses = forwarded.split(",");
            return addresses[addresses.length - 1].trim();
        }

        return request.getRemoteAddr();
    }

    private String hashEmail(String email) {
        String normalized = AppUser.normalizeEmail(email);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private record WindowCounter(int count, long resetAtEpochSecond) {}
    private record Decision(boolean allowed, long retryAfterSeconds) {}
}
