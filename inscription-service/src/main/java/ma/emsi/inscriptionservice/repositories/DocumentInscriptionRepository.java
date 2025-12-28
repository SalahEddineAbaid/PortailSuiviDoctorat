package ma.emsi.inscriptionservice.repositories;

import ma.emsi.inscriptionservice.entities.DocumentInscription;
import ma.emsi.inscriptionservice.enums.TypeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentInscriptionRepository extends JpaRepository<DocumentInscription, Long> {

    List<DocumentInscription> findByInscriptionId(Long inscriptionId);

    Optional<DocumentInscription> findByInscriptionIdAndTypeDocument(
            Long inscriptionId, TypeDocument typeDocument
    );

    List<DocumentInscription> findByInscriptionIdAndValide(Long inscriptionId, Boolean valide);

    boolean existsByInscriptionIdAndTypeDocument(Long inscriptionId, TypeDocument typeDocument);
}
