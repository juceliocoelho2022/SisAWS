package br.com.sisaws.learning;

import br.com.sisaws.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceProgressRepository extends JpaRepository<ServiceProgress, Long> {
    Optional<ServiceProgress> findByUserAndAwsServiceIgnoreCase(AppUser user, String awsService);
    List<ServiceProgress> findAllByUserOrderByAwsServiceAsc(AppUser user);
}
