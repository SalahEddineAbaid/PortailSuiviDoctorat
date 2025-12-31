package ma.emsi.inscriptionservice.repositories;

import ma.emsi.inscriptionservice.entities.Inscription;
import ma.emsi.inscriptionservice.enums.StatutInscription;
import ma.emsi.inscriptionservice.enums.TypeInscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InscriptionRepository extends JpaRepository<Inscription, Long> {

    List<Inscription> findByDoctorantId(Long doctorantId);

    List<Inscription> findByDirecteurTheseId(Long directeurTheseId);

    List<Inscription> findByStatut(StatutInscription statut);

    List<Inscription> findByStatutIn(List<StatutInscription> statuts);

    Optional<Inscription> findByDoctorantIdAndType(Long doctorantId, TypeInscription type);

    @Query("SELECT i FROM Inscription i WHERE i.doctorantId = :doctorantId " +
            "AND i.type = :type AND i.anneeInscription = :annee")
    Optional<Inscription> findInscriptionByDoctorantAndAnnee(
            Long doctorantId, TypeInscription type, Integer annee
    );

    @Query("SELECT i FROM Inscription i WHERE i.doctorantId = :doctorantId " +
            "AND i.type = 'PREMIERE_INSCRIPTION'")
    Optional<Inscription> findPremiereInscriptionByDoctorant(Long doctorantId);

    @Query("SELECT i FROM Inscription i WHERE i.statut = 'EN_ATTENTE_DIRECTEUR' " +
            "AND i.directeurTheseId = :directeurId")
    List<Inscription> findInscriptionsEnAttenteValidation(Long directeurId);

    @Query("SELECT i FROM Inscription i WHERE i.dateCreation < :dateLimit " +
            "AND i.statut = 'BROUILLON'")
    List<Inscription> findBrouillonsExpires(LocalDateTime dateLimit);

    @Query("SELECT COUNT(i) FROM Inscription i WHERE i.doctorantId = :doctorantId " +
            "AND i.statut = 'VALIDE'")
    Long countInscriptionsValidees(Long doctorantId);

    List<Inscription> findByCampagneId(Long campagneId);

    @Query("SELECT i FROM Inscription i WHERE i.campagne.id = :campagneId AND i.statut = :statut")
    List<Inscription> findByCampagneIdAndStatut(Long campagneId, StatutInscription statut);

    @Query("SELECT COUNT(i) FROM Inscription i WHERE i.campagne.id = :campagneId AND i.statut = :statut")
    Long countByCampagneIdAndStatut(Long campagneId, StatutInscription statut);
}
