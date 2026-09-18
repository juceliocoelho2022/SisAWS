package br.com.sisaws.simulation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SimulationAttemptRepository extends JpaRepository<SimulationAttempt, Long> {
    List<SimulationAttempt> findTop10ByOrderByFinishedAtDesc();
}
