package br.com.sisaws.auth;

import br.com.sisaws.user.AppUser;
import br.com.sisaws.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock AppUserRepository userRepository;
    @Mock PasswordResetTokenRepository tokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock PasswordResetNotifier notifier;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(userRepository, tokenRepository, passwordEncoder, notifier, 15);
    }

    @Test
    void shouldCreateHashedOneTimeTokenForExistingUser() {
        AppUser user = new AppUser("Aluno", "aluno@email.com", "hash-antigo");
        when(userRepository.findByEmailIgnoreCase("aluno@email.com")).thenReturn(Optional.of(user));

        service.requestReset(" ALUNO@EMAIL.COM ");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        ArgumentCaptor<String> rawCaptor = ArgumentCaptor.forClass(String.class);

        verify(tokenRepository).deleteAllByUser(user);
        verify(tokenRepository).save(tokenCaptor.capture());
        verify(notifier).sendResetLink(eq(user), rawCaptor.capture());

        assertNotEquals(rawCaptor.getValue(), tokenCaptor.getValue().getTokenHash());
        assertEquals(PasswordResetService.hashToken(rawCaptor.getValue()), tokenCaptor.getValue().getTokenHash());
    }

    @Test
    void shouldNotCreateTokenForUnknownAccount() {
        when(userRepository.findByEmailIgnoreCase("unknown@email.com")).thenReturn(Optional.empty());

        service.requestReset("unknown@email.com");

        verifyNoInteractions(tokenRepository, passwordEncoder, notifier);
    }

    @Test
    void shouldResetPasswordAndInvalidateTokens() {
        AppUser user = new AppUser("Aluno", "aluno@email.com", "hash-antigo");
        String rawToken = "valid-token";
        PasswordResetToken token = new PasswordResetToken(
                user,
                PasswordResetService.hashToken(rawToken),
                LocalDateTime.now().plusMinutes(10)
        );

        when(tokenRepository.findByTokenHash(PasswordResetService.hashToken(rawToken))).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NovaSenha123")).thenReturn("hash-novo");

        service.resetPassword(rawToken, "NovaSenha123");

        assertEquals("hash-novo", user.getPassword());
        verify(tokenRepository).deleteAllByUser(user);
    }

    @Test
    void shouldRejectExpiredToken() {
        AppUser user = new AppUser("Aluno", "aluno@email.com", "hash-antigo");
        String rawToken = "expired-token";
        PasswordResetToken token = new PasswordResetToken(
                user,
                PasswordResetService.hashToken(rawToken),
                LocalDateTime.now().minusMinutes(1)
        );

        when(tokenRepository.findByTokenHash(PasswordResetService.hashToken(rawToken))).thenReturn(Optional.of(token));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.resetPassword(rawToken, "NovaSenha123")
        );

        assertEquals(400, exception.getStatusCode().value());
        verify(tokenRepository).delete(token);
        verify(passwordEncoder, never()).encode(anyString());
    }
}
