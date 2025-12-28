package ma.emsi.inscriptionservice.repositories;

import ma.emsi.inscriptionservice.entities.InfosDoctorant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InfosDoctorantRepository extends JpaRepository<InfosDoctorant, Long> {

    Optional<InfosDoctorant> findByInscriptionId(Long inscriptionId);

    boolean existsByCin(String cin);
}
