package br.com.sisaws.material;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {
    List<StudyMaterial> findAllByActiveTrueOrderByCreatedAtDesc();
}
