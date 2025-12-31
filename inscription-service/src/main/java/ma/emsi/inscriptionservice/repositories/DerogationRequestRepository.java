package ma.emsi.inscriptionservice.repositories;

import ma.emsi.inscriptionservice.entities.DerogationRequest;
import ma.emsi.inscriptionservice.enums.StatutDerogation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DerogationRequestRepository extends JpaRepository<DerogationRequest, Long> {

    Optional<DerogationRequest> findByInscriptionId(Long inscriptionId);

    List<DerogationRequest> findByStatut(StatutDerogation statut);

    @Query("SELECT d FROM DerogationRequest d WHERE d.inscription.directeurTheseId = :directeurId " +
            "AND d.statut = 'EN_ATTENTE'")
    List<DerogationRequest> findDerogationsEnAttenteDirecteur(Long directeurId);

    @Query("SELECT d FROM DerogationRequest d WHERE d.statut = 'APPROUVE_DIRECTEUR'")
    List<DerogationRequest> findDerogationsEnAttentePED();

    @Query("SELECT d FROM DerogationRequest d WHERE d.inscription.id = :inscriptionId " +
            "AND d.statut = 'APPROUVE_PED'")
    Optional<DerogationRequest> findApprovedDerogationByInscription(Long inscriptionId);

    @Query("SELECT d FROM DerogationRequest d WHERE d.inscription.doctorantId = :doctorantId " +
            "ORDER BY d.dateDemande DESC")
    List<DerogationRequest> findByDoctorantId(Long doctorantId);
}
