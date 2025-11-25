package ma.emsi.defenseservice.service;

import ma.emsi.defenseservice.entity.Document;
import ma.emsi.defenseservice.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    public Document upload(Document doc) {
        doc.setUploadDate(LocalDateTime.now());
        return documentRepository.save(doc);
    }

    public List<Document> getByDefenseRequest(Long requestId) {
        return documentRepository.findByDefenseRequestId(requestId);
    }

    public void delete(Long id) {
        documentRepository.deleteById(id);
    }
}

