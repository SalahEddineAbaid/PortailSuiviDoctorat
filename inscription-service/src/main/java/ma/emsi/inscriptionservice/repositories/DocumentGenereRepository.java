package ma.emsi.inscriptionservice.repositories;

import ma.emsi.inscriptionservice.entities.DocumentGenere;
import ma.emsi.inscriptionservice.enums.TypeDocumentGenere;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentGenereRepository extends JpaRepository<DocumentGenere, Long> {

    List<DocumentGenere> findByInscriptionId(Long inscriptionId);

    Optional<DocumentGenere> findByInscriptionIdAndType(Long inscriptionId, TypeDocumentGenere type);

    @Query("SELECT d FROM DocumentGenere d WHERE d.inscription.doctorantId = :doctorantId " +
            "ORDER BY d.dateGeneration DESC")
    List<DocumentGenere> findByDoctorantId(Long doctorantId);

    @Query("SELECT d FROM DocumentGenere d WHERE d.type = :type " +
            "ORDER BY d.dateGeneration DESC")
    List<DocumentGenere> findByType(TypeDocumentGenere type);
}
