package br.com.sisaws.material;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialChunkRepository extends JpaRepository<MaterialChunk, Long> {
    List<MaterialChunk> findAllByMaterialOrderByChunkIndexAsc(StudyMaterial material);
    void deleteAllByMaterial(StudyMaterial material);
}
