package br.com.sisaws.material;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MaterialAiProfileRepository extends JpaRepository<MaterialAiProfile, Long> {
    Optional<MaterialAiProfile> findByMaterial(StudyMaterial material);

    @Modifying
    @Query("delete from MaterialAiProfile profile where profile.material.id = :materialId")
    int deleteByMaterialId(@Param("materialId") Long materialId);
}
