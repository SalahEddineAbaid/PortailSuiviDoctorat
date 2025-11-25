package ma.emsi.defenseservice.repository;

import ma.emsi.defenseservice.entity.Document;
import ma.emsi.defenseservice.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByDefenseRequestId(Long defenseRequestId);

    List<Document> findByDefenseRequestIdAndType(Long defenseRequestId, DocumentType type);
}
