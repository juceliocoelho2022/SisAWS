package br.com.sisaws.certification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CertificationRepository extends JpaRepository<Certification, Long> {
    Optional<Certification> findByCodeIgnoreCase(String code);
}
