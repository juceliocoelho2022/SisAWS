package br.com.sisaws.errornotebook;

import br.com.sisaws.question.Question;
import br.com.sisaws.user.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ErrorNotebookRepository extends JpaRepository<ErrorNotebookEntry, Long> {

    Optional<ErrorNotebookEntry> findByUserAndQuestion(AppUser user, Question question);

    @EntityGraph(attributePaths = "question")
    List<ErrorNotebookEntry> findAllByUserOrderByLastWrongAtDesc(AppUser user);

    long countByUser(AppUser user);
}
