package ma.emsi.inscriptionservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.inscriptionservice.DTOs.DocumentResponse;
import ma.emsi.inscriptionservice.entities.DocumentInscription;
import ma.emsi.inscriptionservice.entities.Inscription;
import ma.emsi.inscriptionservice.enums.TypeDocument;
import ma.emsi.inscriptionservice.repositories.DocumentInscriptionRepository;
import ma.emsi.inscriptionservice.repositories.InscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentInscriptionRepository documentRepository;
    private final InscriptionRepository inscriptionRepository;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Transactional
    public DocumentResponse uploadDocument(Long inscriptionId, MultipartFile file,
                                           TypeDocument typeDocument) {
        log.info("Upload document {} pour inscription {}", typeDocument, inscriptionId);

        // Vérifier que l'inscription existe
        Inscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

        // Vérifier le type de fichier
        String contentType = file.getContentType();
        if (!contentType.equals("application/pdf") && !contentType.startsWith("image/")) {
            throw new RuntimeException("Type de fichier non autorisé");
        }

        // Générer un nom unique
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

        try {
            // Créer le dossier s'il n'existe pas
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Sauvegarder le fichier
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Créer l'entrée en base
            DocumentInscription document = DocumentInscription.builder()
                    .inscription(inscription)
                    .typeDocument(typeDocument)
                    .nomFichier(file.getOriginalFilename())
                    .cheminFichier(filePath.toString())
                    .tailleFichier(file.getSize())
                    .mimeType(contentType)
                    .valide(false)
                    .build();

            document = documentRepository.save(document);

            log.info("Document uploadé avec succès: {}", document.getId());

            return mapToResponse(document);

        } catch (IOException e) {
            log.error("Erreur lors de l'upload du fichier", e);
            throw new RuntimeException("Erreur lors de l'upload du fichier");
        }
    }

    public List<DocumentResponse> getDocuments(Long inscriptionId) {
        return documentRepository.findByInscriptionId(inscriptionId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Resource downloadDocument(Long documentId) {
        DocumentInscription document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document introuvable"));

        try {
            Path filePath = Paths.get(document.getCheminFichier());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Fichier introuvable ou illisible");
            }
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la lecture du fichier");
        }
    }

    @Transactional
    public void deleteDocument(Long documentId) {
        DocumentInscription document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document introuvable"));

        try {
            Files.deleteIfExists(Paths.get(document.getCheminFichier()));
            documentRepository.delete(document);

            log.info("Document supprimé: {}", documentId);
        } catch (IOException e) {
            log.error("Erreur lors de la suppression du fichier", e);
            throw new RuntimeException("Erreur lors de la suppression du fichier");
        }
    }

    private DocumentResponse mapToResponse(DocumentInscription document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .typeDocument(document.getTypeDocument())
                .nomFichier(document.getNomFichier())
                .tailleFichier(document.getTailleFichier())
                .mimeType(document.getMimeType())
                .dateUpload(document.getDateUpload())
                .valide(document.getValide())
                .commentaire(document.getCommentaire())
                .build();
    }
}
