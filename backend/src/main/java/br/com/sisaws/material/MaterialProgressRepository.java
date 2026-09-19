package br.com.sisaws.material;

import br.com.sisaws.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialProgressRepository extends JpaRepository<MaterialProgress, Long> {
    List<MaterialProgress> findAllByUser(AppUser user);
    Optional<MaterialProgress> findByUserAndChapter(AppUser user, StudyChapter chapter);
}
