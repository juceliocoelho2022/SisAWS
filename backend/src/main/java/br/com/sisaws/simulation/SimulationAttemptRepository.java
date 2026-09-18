package br.com.sisaws.simulation;

import br.com.sisaws.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimulationAttemptRepository extends JpaRepository<SimulationAttempt, Long> {
    List<SimulationAttempt> findTop10ByUserOrderByFinishedAtDesc(AppUser user);
    List<SimulationAttempt> findAllByUser(AppUser user);
}
