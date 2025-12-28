package ma.emsi.inscriptionservice.repositories;

import ma.emsi.inscriptionservice.entities.ValidationInscription;
import ma.emsi.inscriptionservice.enums.StatutValidation;
import ma.emsi.inscriptionservice.enums.TypeValidateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ValidationInscriptionRepository extends JpaRepository<ValidationInscription, Long> {

    List<ValidationInscription> findByInscriptionId(Long inscriptionId);

    Optional<ValidationInscription> findByInscriptionIdAndTypeValidateur(
            Long inscriptionId, TypeValidateur typeValidateur
    );

    List<ValidationInscription> findByValidateurIdAndStatut(
            Long validateurId, StatutValidation statut
    );

    List<ValidationInscription> findByInscriptionIdAndStatut(
            Long inscriptionId, StatutValidation statut
    );
}
