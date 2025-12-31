package ma.emsi.inscriptionservice.repositories;

import ma.emsi.inscriptionservice.entities.AlerteDuree;
import ma.emsi.inscriptionservice.enums.TypeAlerte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlerteDureeRepository extends JpaRepository<AlerteDuree, Long> {

    List<AlerteDuree> findByInscriptionId(Long inscriptionId);

    @Query("SELECT a FROM AlerteDuree a WHERE a.inscription.doctorantId = :doctorantId " +
            "AND a.traite = false ORDER BY a.dateAlerte DESC")
    List<AlerteDuree> findAlertesActivesByDoctorant(Long doctorantId);

    @Query("SELECT a FROM AlerteDuree a WHERE a.inscription.id = :inscriptionId " +
            "AND a.type = :type")
    Optional<AlerteDuree> findByInscriptionIdAndType(Long inscriptionId, TypeAlerte type);

    @Query("SELECT a FROM AlerteDuree a WHERE a.traite = false")
    List<AlerteDuree> findAllActiveAlertes();

    @Query("SELECT COUNT(a) FROM AlerteDuree a WHERE a.inscription.id = :inscriptionId " +
            "AND a.type = :type")
    Long countByInscriptionIdAndType(Long inscriptionId, TypeAlerte type);
}
