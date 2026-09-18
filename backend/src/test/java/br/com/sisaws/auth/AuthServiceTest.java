package br.com.sisaws.auth;

import br.com.sisaws.security.JwtService;
import br.com.sisaws.user.AppUser;
import br.com.sisaws.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void shouldRegisterNewStudentWithNormalizedEmail() {
        when(userRepository.existsByEmailIgnoreCase("aluno@email.com")).thenReturn(false);
        when(passwordEncoder.encode("12345678")).thenReturn("encoded");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(AppUser.class))).thenReturn("token");

        AuthService.AuthResult result = authService.register(
                "Aluno SisAWS",
                "  ALUNO@EMAIL.COM ",
                "12345678"
        );

        assertEquals("token", result.token());
        assertEquals("aluno@email.com", result.user().getEmail());
        assertEquals("Aluno SisAWS", result.user().getName());
        assertEquals("encoded", result.user().getPassword());

        verify(userRepository).save(any(AppUser.class));
    }

    @Test
    void shouldRejectDuplicatedEmail() {
        when(userRepository.existsByEmailIgnoreCase("aluno@email.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.register("Aluno", "aluno@email.com", "12345678")
        );

        assertEquals(409, exception.getStatusCode().value());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldLoginWithValidCredentials() {
        AppUser user = new AppUser("Aluno", "aluno@email.com", "encoded");
        when(userRepository.findByEmailIgnoreCase("aluno@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("12345678", "encoded")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthService.AuthResult result = authService.login("ALUNO@EMAIL.COM", "12345678");

        assertEquals("jwt-token", result.token());
        assertSame(user, result.user());
    }

    @Test
    void shouldRejectInvalidPassword() {
        AppUser user = new AppUser("Aluno", "aluno@email.com", "encoded");
        when(userRepository.findByEmailIgnoreCase("aluno@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha-errada", "encoded")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login("aluno@email.com", "senha-errada")
        );

        assertEquals(401, exception.getStatusCode().value());
        verify(jwtService, never()).generateToken(any());
    }
}
