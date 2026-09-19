package br.com.sisaws.material;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialAiFlashcardRepository extends JpaRepository<MaterialAiFlashcard, Long> {
    List<MaterialAiFlashcard> findAllByMaterialOrderByIdAsc(StudyMaterial material);
    void deleteAllByMaterial(StudyMaterial material);
}
