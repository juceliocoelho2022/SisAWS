package br.com.sisaws.material;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MaterialAiProfileRepository extends JpaRepository<MaterialAiProfile, Long> {
    Optional<MaterialAiProfile> findByMaterial(StudyMaterial material);
    void deleteByMaterial(StudyMaterial material);
}
