package br.com.sisaws.learning;

import br.com.sisaws.certification.Certification;
import br.com.sisaws.errornotebook.ErrorNotebookEntry;
import br.com.sisaws.errornotebook.ErrorNotebookRepository;
import br.com.sisaws.question.Difficulty;
import br.com.sisaws.question.Question;
import br.com.sisaws.user.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdaptiveLearningControllerTest {

    @Mock
    private ServiceProgressRepository progressRepository;

    @Mock
    private ErrorNotebookRepository errorNotebookRepository;

    @Mock
    private Authentication authentication;

    private AdaptiveLearningController controller;
    private AppUser user;

    @BeforeEach
    void setUp() {
        controller = new AdaptiveLearningController(progressRepository, errorNotebookRepository);
        user = new AppUser("Aluno", "aluno@email.com", "encoded");
        when(authentication.getPrincipal()).thenReturn(user);
    }

    @Test
    void shouldSuggestIamForNewStudent() {
        when(progressRepository.findAllByUserOrderByAwsServiceAsc(user)).thenReturn(List.of());
        when(errorNotebookRepository.findAllByUserOrderByLastWrongAtDesc(user)).thenReturn(List.of());

        AdaptiveLearningController.AdaptivePlanResponse plan = controller.plan(authentication);

        assertEquals("FOUNDATION", plan.overallLevel());
        assertEquals("IAM", plan.nextAction().awsService());
        assertEquals("EASY", plan.nextAction().difficulty());
        assertEquals(5, plan.nextAction().questionCount());
        assertFalse(plan.focusServices().isEmpty());
    }

    @Test
    void shouldPrioritizeServiceWithLowAccuracyAndRepeatedErrors() {
        ServiceProgress s3 = new ServiceProgress(user, "S3");
        s3.recordAnswer(false);
        s3.recordAnswer(false);
        s3.recordAnswer(true);
        s3.recordAnswer(false);

        ServiceProgress ec2 = new ServiceProgress(user, "EC2");
        ec2.recordAnswer(true);
        ec2.recordAnswer(true);
        ec2.recordAnswer(true);
        ec2.recordAnswer(true);

        Certification certification = new Certification("SAA-C03", "Solutions Architect Associate");
        Question question = new Question(
                certification,
                "Design Cost-Optimized Architectures",
                "S3",
                Difficulty.MEDIUM,
                "Pergunta sobre S3",
                "Explicação"
        );

        ErrorNotebookEntry repeatedError = new ErrorNotebookEntry(user, question);
        repeatedError.recordWrongAnswer();
        repeatedError.recordWrongAnswer();

        when(progressRepository.findAllByUserOrderByAwsServiceAsc(user)).thenReturn(List.of(s3, ec2));
        when(errorNotebookRepository.findAllByUserOrderByLastWrongAtDesc(user))
                .thenReturn(List.of(repeatedError));

        AdaptiveLearningController.AdaptivePlanResponse plan = controller.plan(authentication);

        assertEquals("S3", plan.focusServices().getFirst().awsService());
        assertEquals("S3", plan.nextAction().awsService());
        assertTrue(plan.focusServices().getFirst().priorityScore() > 50);
        assertEquals("EASY", plan.nextAction().difficulty());
    }
}
