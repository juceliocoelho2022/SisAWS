package br.com.sisaws.material;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyChapterRepository extends JpaRepository<StudyChapter, Long> {
    List<StudyChapter> findAllByMaterialOrderByChapterNumberAsc(StudyMaterial material);
    Optional<StudyChapter> findByIdAndMaterial(Long id, StudyMaterial material);
    boolean existsByMaterialAndChapterNumber(StudyMaterial material, int chapterNumber);
}
