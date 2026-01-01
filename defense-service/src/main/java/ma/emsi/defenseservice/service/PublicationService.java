package ma.emsi.defenseservice.service;

import ma.emsi.defenseservice.dto.request.PublicationCreateDTO;
import ma.emsi.defenseservice.dto.request.ValidationDTO;
import ma.emsi.defenseservice.dto.response.PublicationResponseDTO;
import ma.emsi.defenseservice.entity.Prerequisites;
import ma.emsi.defenseservice.entity.Publication;
import ma.emsi.defenseservice.enums.QuartileJournal;
import ma.emsi.defenseservice.exception.ResourceNotFoundException;
import ma.emsi.defenseservice.mapper.PublicationMapper;
import ma.emsi.defenseservice.repository.PrerequisitesRepository;
import ma.emsi.defenseservice.repository.PublicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PublicationService {

    @Autowired
    private PublicationRepository publicationRepository;

    @Autowired
    private PrerequisitesRepository prerequisitesRepository;

    @Autowired
    private PublicationMapper publicationMapper;

    /**
     * Create a new publication
     * Requirement 1.1: Store publication with all fields
     * Requirement 1.2: Set validation status to false by default
     */
    @Transactional
    public PublicationResponseDTO createPublication(PublicationCreateDTO dto) {
        // Verify prerequisites exists
        Prerequisites prerequisites = prerequisitesRepository.findById(dto.getPrerequisitesId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Prerequisites not found with id: " + dto.getPrerequisitesId()));

        // Convert DTO to entity
        Publication publication = publicationMapper.toEntity(dto);
        publication.setPrerequisites(prerequisites);

        // Save publication
        Publication savedPublication = publicationRepository.save(publication);

        return publicationMapper.toDTO(savedPublication);
    }

    /**
     * Get all publications for a given prerequisites
     * Requirement 1.1: Retrieve publications by prerequisites
     */
    public List<PublicationResponseDTO> getPublicationsByPrerequisites(Long prerequisitesId) {
        List<Publication> publications = publicationRepository.findByPrerequisitesId(prerequisitesId);
        return publications.stream()
                .map(publicationMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Validate a publication
     * Requirement 1.3: Record validator ID, verification date, verified quartile,
     * and validation comments
     * Requirement 1.8: Allow updating the quartile if verification reveals a
     * different classification
     */
    @Transactional
    public PublicationResponseDTO validatePublication(Long id, ValidationDTO dto) {
        Publication publication = publicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Publication not found with id: " + id));

        // Update validation fields
        publication.setValide(true);
        publication.setValidateurId(dto.getValidateurId());
        publication.setQuartile(dto.getQuartile()); // Allow updating quartile during validation
        publication.setCommentaireValidation(dto.getCommentaireValidation());
        publication.setDateValidation(LocalDateTime.now());

        Publication savedPublication = publicationRepository.save(publication);

        return publicationMapper.toDTO(savedPublication);
    }

    /**
     * Get all publications pending validation
     * Requirement 1.5: Return all publications where validation status is false
     */
    public List<PublicationResponseDTO> getPendingValidations() {
        List<Publication> pendingPublications = publicationRepository.findByValide(false);
        return pendingPublications.stream()
                .map(publicationMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Check if prerequisites has the minimum required valid Q1/Q2 publications
     * Requirement 1.6: Verify at least minRequired validated journal articles with
     * Q1 or Q2 quartile exist
     */
    public boolean hasValidQ1Q2Publications(Long prerequisitesId, int minRequired) {
        long count = countValidQ1Q2Publications(prerequisitesId);
        return count >= minRequired;
    }

    /**
     * Count valid Q1/Q2 publications for a given prerequisites
     * Requirement 1.4: Only include publications where validation status is true
     * and quartile is Q1 or Q2
     */
    public long countValidQ1Q2Publications(Long prerequisitesId) {
        return publicationRepository.countValidQ1Q2Publications(
                prerequisitesId,
                QuartileJournal.Q1,
                QuartileJournal.Q2);
    }
}
