package br.com.sisaws.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class AuthRateLimiterTest {

    @Test
    void shouldLimitLoginByIp() {
        AuthRateLimiter limiter = new AuthRateLimiter(2, 50, 50, 50, 50, 50);
        MockHttpServletRequest request = request("203.0.113.10");

        limiter.checkLogin(request, "aluno@email.com");
        limiter.checkLogin(request, "outro@email.com");

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> limiter.checkLogin(request, "terceiro@email.com")
        );

        assertTrue(exception.getRetryAfterSeconds() > 0);
    }

    @Test
    void shouldLimitLoginByNormalizedEmailAcrossDifferentIps() {
        AuthRateLimiter limiter = new AuthRateLimiter(50, 2, 50, 50, 50, 50);

        limiter.checkLogin(request("203.0.113.11"), " ALUNO@EMAIL.COM ");
        limiter.checkLogin(request("203.0.113.12"), "aluno@email.com");

        assertThrows(
                RateLimitExceededException.class,
                () -> limiter.checkLogin(request("203.0.113.13"), "aluno@email.com")
        );
    }

    @Test
    void shouldLimitForgotPasswordWithoutDependingOnAccountExistence() {
        AuthRateLimiter limiter = new AuthRateLimiter(50, 50, 50, 1, 50, 50);

        limiter.checkForgotPassword(request("203.0.113.20"), "unknown@email.com");

        assertThrows(
                RateLimitExceededException.class,
                () -> limiter.checkForgotPassword(request("203.0.113.21"), "unknown@email.com")
        );
    }

    @Test
    void shouldUseLastForwardedAddressFromAlbChain() {
        AuthRateLimiter limiter = new AuthRateLimiter(1, 50, 50, 50, 50, 50);
        MockHttpServletRequest first = request("10.0.0.10");
        first.addHeader("X-Forwarded-For", "198.51.100.99, 203.0.113.30");

        MockHttpServletRequest second = request("10.0.0.11");
        second.addHeader("X-Forwarded-For", "192.0.2.44, 203.0.113.30");

        limiter.checkLogin(first, "a@email.com");

        assertThrows(
                RateLimitExceededException.class,
                () -> limiter.checkLogin(second, "b@email.com")
        );
    }

    private MockHttpServletRequest request(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
