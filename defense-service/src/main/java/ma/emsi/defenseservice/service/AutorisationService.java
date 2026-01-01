package ma.emsi.defenseservice.service;

import ma.emsi.defenseservice.config.DefenseConfigProperties;
import ma.emsi.defenseservice.dto.request.AutorisationDTO;
import ma.emsi.defenseservice.dto.request.RefusDTO;
import ma.emsi.defenseservice.dto.response.AutorisationSoutenanceDTO;
import ma.emsi.defenseservice.dto.response.VerificationResultDTO;
import ma.emsi.defenseservice.entity.*;
import ma.emsi.defenseservice.enums.DefenseRequestStatus;
import ma.emsi.defenseservice.enums.DefenseStatus;
import ma.emsi.defenseservice.enums.MemberRole;
import ma.emsi.defenseservice.enums.StatutAutorisation;
import ma.emsi.defenseservice.exception.AuthorizationAlreadyExistsException;
import ma.emsi.defenseservice.exception.ResourceNotFoundException;
import ma.emsi.defenseservice.mapper.AutorisationMapper;
import ma.emsi.defenseservice.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AutorisationService {

    @Autowired
    private AutorisationSoutenanceRepository autorisationRepository;

    @Autowired
    private DefenseRequestRepository defenseRequestRepository;

    @Autowired
    private DefenseRepository defenseRepository;

    @Autowired
    private JuryRepository juryRepository;

    @Autowired
    private RapportRepository rapportRepository;

    @Autowired
    private PublicationService publicationService;

    @Autowired
    private AutorisationMapper autorisationMapper;

    @Autowired
    private DefenseEventPublisher defenseEventPublisher;

    @Autowired
    private DefenseConfigProperties defenseConfig;

    /**
     * Verify all prerequisites for authorization
     * Requirements: 2.1, 2.11
     */
    public VerificationResultDTO verifierPrerequisAutorisation(Long defenseRequestId) {
        DefenseRequest defenseRequest = defenseRequestRepository.findById(defenseRequestId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Defense request not found with id: " + defenseRequestId));

        VerificationResultDTO result = new VerificationResultDTO();

        // Check 1: Prerequisites validated
        boolean prerequisValides = verifierPrerequisites(defenseRequest);
        result.getVerificationsDetaillees().put("prerequis_valides", prerequisValides);
        if (!prerequisValides) {
            result.getBlocages().add("Prerequisites are not validated or do not meet minimum requirements");
        }

        // Check 2: Jury complete
        boolean juryComplet = verifierJuryComplet(defenseRequest);
        result.getVerificationsDetaillees().put("jury_complet", juryComplet);
        if (!juryComplet) {
            result.getBlocages().add("Jury composition is incomplete");
        }

        // Check 3: Reports favorable
        boolean rapportsFavorables = verifierRapportsFavorables(defenseRequest);
        result.getVerificationsDetaillees().put("rapports_favorables", rapportsFavorables);
        if (!rapportsFavorables) {
            result.getBlocages().add("Not all rapporteurs have submitted favorable reports");
        }

        // Check 4: Documents complete
        boolean documentsComplets = verifierDocumentsComplets(defenseRequest);
        result.getVerificationsDetaillees().put("documents_complets", documentsComplets);
        if (!documentsComplets) {
            result.getBlocages().add("Required documents are not uploaded");
        }

        // Can authorize only if all checks pass
        result.setPeutAutoriser(prerequisValides && juryComplet && rapportsFavorables && documentsComplets);

        return result;
    }

    /**
     * Authorize a defense
     * Requirements: 2.4, 2.5, 2.6, 2.12
     */
    @Transactional
    public AutorisationSoutenanceDTO autoriser(Long defenseRequestId, AutorisationDTO dto) {
        DefenseRequest defenseRequest = defenseRequestRepository.findById(defenseRequestId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Defense request not found with id: " + defenseRequestId));

        // Check if authorization already exists
        if (autorisationRepository.findByDefenseRequestId(defenseRequestId).isPresent()) {
            throw new AuthorizationAlreadyExistsException(
                    "Authorization already exists for defense request: " + defenseRequestId);
        }

        // Verify prerequisites
        VerificationResultDTO verification = verifierPrerequisAutorisation(defenseRequestId);
        if (!verification.isPeutAutoriser()) {
            throw new IllegalStateException(
                    "Cannot authorize defense. Verification failed: " + verification.getBlocages());
        }

        // Create authorization entity
        AutorisationSoutenance autorisation = new AutorisationSoutenance();
        autorisation.setDefenseRequest(defenseRequest);
        autorisation.setStatut(StatutAutorisation.AUTORISE);
        autorisation.setAdministrateurId(dto.getAdministrateurId());
        autorisation.setDateAutorisation(LocalDateTime.now());
        autorisation.setPrerequisValides(verification.getVerificationsDetaillees().get("prerequis_valides"));
        autorisation.setJuryComplet(verification.getVerificationsDetaillees().get("jury_complet"));
        autorisation.setRapportsFavorables(verification.getVerificationsDetaillees().get("rapports_favorables"));
        autorisation.setDocumentsComplets(verification.getVerificationsDetaillees().get("documents_complets"));
        autorisation.setCommentaireAdmin(dto.getCommentaireAdmin());
        autorisation.setDateSoutenance(dto.getDateSoutenance());
        autorisation.setLieuSoutenance(dto.getLieuSoutenance());
        autorisation.setSalleSoutenance(dto.getSalleSoutenance());

        autorisation = autorisationRepository.save(autorisation);

        // Update defense request status
        defenseRequest.setStatus(DefenseRequestStatus.AUTHORIZED);
        defenseRequestRepository.save(defenseRequest);

        // Create Defense entity
        Defense defense = new Defense();
        defense.setDefenseRequest(defenseRequest);
        defense.setDefenseDate(dto.getDateSoutenance());
        defense.setLocation(dto.getLieuSoutenance());
        defense.setRoom(dto.getSalleSoutenance());
        defense.setStatus(DefenseStatus.SCHEDULED);
        defenseRepository.save(defense);

        // Publish SOUTENANCE_AUTORISEE event (Requirement 2.7)
        defenseEventPublisher.publishDefenseAuthorized(autorisation);

        return autorisationMapper.toDTO(autorisation);
    }

    /**
     * Refuse authorization
     * Requirements: 2.8, 2.9, 2.12
     */
    @Transactional
    public AutorisationSoutenanceDTO refuser(Long defenseRequestId, RefusDTO dto) {
        DefenseRequest defenseRequest = defenseRequestRepository.findById(defenseRequestId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Defense request not found with id: " + defenseRequestId));

        // Check if authorization already exists
        if (autorisationRepository.findByDefenseRequestId(defenseRequestId).isPresent()) {
            throw new AuthorizationAlreadyExistsException(
                    "Authorization already exists for defense request: " + defenseRequestId);
        }

        // Verify prerequisites to get detailed results
        VerificationResultDTO verification = verifierPrerequisAutorisation(defenseRequestId);

        // Create refusal authorization entity
        AutorisationSoutenance autorisation = new AutorisationSoutenance();
        autorisation.setDefenseRequest(defenseRequest);
        autorisation.setStatut(StatutAutorisation.REFUSE);
        autorisation.setAdministrateurId(dto.getAdministrateurId());
        autorisation.setDateAutorisation(LocalDateTime.now());
        autorisation.setPrerequisValides(verification.getVerificationsDetaillees().get("prerequis_valides"));
        autorisation.setJuryComplet(verification.getVerificationsDetaillees().get("jury_complet"));
        autorisation.setRapportsFavorables(verification.getVerificationsDetaillees().get("rapports_favorables"));
        autorisation.setDocumentsComplets(verification.getVerificationsDetaillees().get("documents_complets"));
        autorisation.setMotifRefus(dto.getMotifRefus());
        autorisation.setCommentaireAdmin(dto.getCommentaireAdmin());

        autorisation = autorisationRepository.save(autorisation);

        // Update defense request status
        defenseRequest.setStatus(DefenseRequestStatus.REJECTED);
        defenseRequest.setRejectionReason(dto.getMotifRefus());
        defenseRequestRepository.save(defenseRequest);

        // Publish SOUTENANCE_REFUSEE event (Requirement 2.10)
        defenseEventPublisher.publishDefenseRefused(autorisation);

        return autorisationMapper.toDTO(autorisation);
    }

    /**
     * Get authorization details
     */
    public AutorisationSoutenanceDTO getAutorisation(Long defenseRequestId) {
        AutorisationSoutenance autorisation = autorisationRepository.findByDefenseRequestId(defenseRequestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authorization not found for defense request: " + defenseRequestId));

        return autorisationMapper.toDTO(autorisation);
    }

    /**
     * Helper method: Verify prerequisites are validated
     * Requirements: 2.1
     */
    private boolean verifierPrerequisites(DefenseRequest defenseRequest) {
        Prerequisites prerequisites = defenseRequest.getPrerequisites();
        if (prerequisites == null || !prerequisites.isValid()) {
            return false;
        }

        // Check Q1/Q2 publications using configuration
        return publicationService.hasValidQ1Q2Publications(
                prerequisites.getId(),
                defenseConfig.getPrerequis().getMinPublicationsQ1Q2());
    }

    /**
     * Helper method: Verify jury is complete
     * Requirements: 2.2
     */
    private boolean verifierJuryComplet(DefenseRequest defenseRequest) {
        Jury jury = juryRepository.findByDefenseRequestId(defenseRequest.getId()).orElse(null);
        if (jury == null || jury.getMembers() == null || jury.getMembers().isEmpty()) {
            return false;
        }

        List<JuryMember> members = jury.getMembers();

        // Check minimum total members using configuration
        if (members.size() < defenseConfig.getJury().getMinMembres()) {
            return false;
        }

        // Check for at least one president
        boolean hasPresident = members.stream()
                .anyMatch(m -> m.getRole() == MemberRole.PRESIDENT);

        // Check for minimum rapporteurs using configuration
        long rapporteurCount = members.stream()
                .filter(m -> m.getRole() == MemberRole.RAPPORTEUR)
                .count();

        // Check for at least one examiner
        boolean hasExaminer = members.stream()
                .anyMatch(m -> m.getRole() == MemberRole.EXAMINATEUR);

        return hasPresident && rapporteurCount >= defenseConfig.getJury().getMinRapporteurs() && hasExaminer;
    }

    /**
     * Helper method: Verify all rapporteurs submitted favorable reports
     * Requirements: 2.3
     */
    private boolean verifierRapportsFavorables(DefenseRequest defenseRequest) {
        Jury jury = juryRepository.findByDefenseRequestId(defenseRequest.getId()).orElse(null);
        if (jury == null) {
            return false;
        }

        // Get all rapporteurs
        List<JuryMember> rapporteurs = jury.getMembers().stream()
                .filter(m -> m.getRole() == MemberRole.RAPPORTEUR)
                .toList();

        if (rapporteurs.isEmpty()) {
            return false;
        }

        // Get all reports for this defense request
        List<Rapport> rapports = rapportRepository.findByDefenseRequestId(defenseRequest.getId());

        // Check that each rapporteur has submitted a favorable report
        for (JuryMember rapporteur : rapporteurs) {
            boolean hasReport = rapports.stream()
                    .anyMatch(r -> r.getJuryMember() != null &&
                            r.getJuryMember().getId().equals(rapporteur.getId()) &&
                            r.isFavorable());

            if (!hasReport) {
                return false;
            }
        }

        return true;
    }

    /**
     * Helper method: Verify all required documents are uploaded
     * Requirements: 2.1
     */
    private boolean verifierDocumentsComplets(DefenseRequest defenseRequest) {
        List<Document> documents = defenseRequest.getDocuments();
        // Basic check: at least some documents are uploaded
        // In a real system, you would check for specific required document types
        return documents != null && !documents.isEmpty();
    }
}
