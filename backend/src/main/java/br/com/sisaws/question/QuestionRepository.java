package br.com.sisaws.question;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @EntityGraph(attributePaths = "options")
    List<Question> findByCertification_CodeIgnoreCase(
            String certificationCode
    );

    @Override
    @EntityGraph(attributePaths = "options")
    Optional<Question> findById(Long id);

    long countByCertification_CodeIgnoreCase(
            String certificationCode
    );
}