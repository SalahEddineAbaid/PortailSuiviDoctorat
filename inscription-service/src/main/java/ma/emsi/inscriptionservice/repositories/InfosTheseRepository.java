package ma.emsi.inscriptionservice.repositories;

import ma.emsi.inscriptionservice.entities.InfosThese;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InfosTheseRepository extends JpaRepository<InfosThese, Long> {

    Optional<InfosThese> findByInscriptionId(Long inscriptionId);
}
