package br.com.sisaws.security;

import br.com.sisaws.learning.ServiceProgress;
import br.com.sisaws.learning.ServiceProgressRepository;
import br.com.sisaws.simulation.SimulationAttempt;
import br.com.sisaws.simulation.SimulationAttemptRepository;
import br.com.sisaws.user.AppUser;
import br.com.sisaws.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserDataIsolationTest {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private SimulationAttemptRepository attemptRepository;

    @Autowired
    private ServiceProgressRepository progressRepository;

    @Test
    void newUserMustNotSeeAnotherUsersAttemptsOrProgress() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        AppUser firstUser = userRepository.save(
                new AppUser("Aluno A", "aluno-a@example.test", encoder.encode("Senha123!"))
        );
        AppUser newUser = userRepository.save(
                new AppUser("Aluno B", "aluno-b@example.test", encoder.encode("Senha123!"))
        );

        attemptRepository.save(
                new SimulationAttempt(firstUser, "SAA-C03", 10, 8, 80.0)
        );

        ServiceProgress firstUserProgress = new ServiceProgress(firstUser, "S3");
        firstUserProgress.recordAnswer(true);
        firstUserProgress.recordAnswer(false);
        progressRepository.save(firstUserProgress);

        assertEquals(1, attemptRepository.findAllByUser(firstUser).size());
        assertEquals(1, progressRepository.findAllByUserOrderByAwsServiceAsc(firstUser).size());

        assertTrue(attemptRepository.findAllByUser(newUser).isEmpty());
        assertTrue(attemptRepository.findTop10ByUserOrderByFinishedAtDesc(newUser).isEmpty());
        assertTrue(progressRepository.findAllByUserOrderByAwsServiceAsc(newUser).isEmpty());
    }
}
