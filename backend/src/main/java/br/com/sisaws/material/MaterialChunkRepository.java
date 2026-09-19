package br.com.sisaws.material;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MaterialChunkRepository extends JpaRepository<MaterialChunk, Long> {
    List<MaterialChunk> findAllByMaterialOrderByChunkIndexAsc(StudyMaterial material);

    @Modifying
    @Query("delete from MaterialChunk chunk where chunk.material.id = :materialId")
    int deleteAllByMaterialId(@Param("materialId") Long materialId);
}
