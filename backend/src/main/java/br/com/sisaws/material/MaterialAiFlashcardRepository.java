package br.com.sisaws.material;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MaterialAiFlashcardRepository extends JpaRepository<MaterialAiFlashcard, Long> {
    List<MaterialAiFlashcard> findAllByMaterialOrderByIdAsc(StudyMaterial material);

    @Modifying
    @Query("delete from MaterialAiFlashcard card where card.material.id = :materialId")
    int deleteAllByMaterialId(@Param("materialId") Long materialId);
}
