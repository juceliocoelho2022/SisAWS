package br.com.sisaws.security;

import br.com.sisaws.user.AppUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET =
            "U2lzQVdTLXRlc3Qtand0LXNlY3JldC1rZXktMjAyNi1mb3ItdGVzdHM=";

    @Test
    void shouldGenerateAndValidateToken() {
        JwtService service = new JwtService(SECRET, 30);
        AppUser user = new AppUser("Aluno SisAWS", "aluno@email.com", "encoded");

        String token = service.generateToken(user);

        assertNotNull(token);
        assertEquals("aluno@email.com", service.extractSubject(token));
        assertTrue(service.isValid(token, user));
    }

    @Test
    void shouldRejectTokenForDifferentUser() {
        JwtService service = new JwtService(SECRET, 30);
        AppUser owner = new AppUser("Aluno A", "a@email.com", "encoded");
        AppUser another = new AppUser("Aluno B", "b@email.com", "encoded");

        String token = service.generateToken(owner);

        assertFalse(service.isValid(token, another));
    }
}
