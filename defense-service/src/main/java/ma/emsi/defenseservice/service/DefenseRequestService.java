package ma.emsi.defenseservice.service;

import ma.emsi.defenseservice.client.UserServiceClient;
import ma.emsi.defenseservice.dto.external.UserDTO;
import ma.emsi.defenseservice.entity.DefenseRequest;
import ma.emsi.defenseservice.entity.Prerequisites;
import ma.emsi.defenseservice.enums.DefenseRequestStatus;
import ma.emsi.defenseservice.exception.ResourceNotFoundException;
import ma.emsi.defenseservice.repository.DefenseRequestRepository;
import ma.emsi.defenseservice.repository.PrerequisitesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DefenseRequestService {

    @Autowired
    private DefenseRequestRepository defenseRequestRepository;

    @Autowired
    private PrerequisitesRepository prerequisitesRepository;

    @Autowired
    private UserServiceClient userServiceClient;

    public DefenseRequest create(DefenseRequest request, Long prerequisitesId) {
        // ✅ VALIDATION : Vérifier que le doctorant existe dans user-service
        try {
            System.out.println("🔍 Tentative de récupération de l'utilisateur avec ID: " + request.getDoctorantId());
            UserDTO user = userServiceClient.getUserById(request.getDoctorantId());
            System.out.println("✅ Utilisateur trouvé: " + user.getEmail() + " avec rôles: " + user.getRoles());

            // Vérifier que l'utilisateur a le rôle DOCTORANT
            if (user.getRoles() == null || !user.getRoles().contains("ROLE_DOCTORANT")) {
                throw new IllegalArgumentException(
                        "L'utilisateur avec l'ID " + request.getDoctorantId() +
                                " n'a pas le rôle DOCTORANT. Rôles: " + user.getRoles());
            }
        } catch (feign.FeignException e) {
            System.err.println("❌ Erreur Feign: " + e.status() + " - " + e.getMessage());
            System.err.println("❌ Contenu de la réponse: " + e.contentUTF8());
            throw new ResourceNotFoundException(
                    "Doctorant avec l'ID " + request.getDoctorantId() +
                            " introuvable dans user-service. Erreur: " + e.status());
        } catch (Exception e) {
            System.err.println("❌ Erreur générale: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            throw new ResourceNotFoundException(
                    "Doctorant avec l'ID " + request.getDoctorantId() +
                            " introuvable dans user-service. Erreur: " + e.getMessage());
        }

        request.setSubmissionDate(LocalDateTime.now());

        // Définir le statut par défaut si non fourni
        if (request.getStatus() == null) {
            request.setStatus(DefenseRequestStatus.SUBMITTED);
        }

        // Si un prerequisitesId est fourni, charger l'entité depuis la DB
        if (prerequisitesId != null) {
            Prerequisites prerequisites = prerequisitesRepository.findById(prerequisitesId)
                    .orElseThrow(() -> new ResourceNotFoundException("Prerequisites not found"));
            request.setPrerequisites(prerequisites);
        }

        return defenseRequestRepository.save(request);
    }

    public DefenseRequest getById(Long id) {
        return defenseRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DefenseRequest not found"));
    }

    public List<DefenseRequest> getAll() {
        return defenseRequestRepository.findAll();
    }

    public DefenseRequest updateStatus(Long id, DefenseRequestStatus status) {
        DefenseRequest req = getById(id);
        req.setStatus(status);
        return defenseRequestRepository.save(req);
    }

    public void delete(Long id) {
        defenseRequestRepository.deleteById(id);
    }
}
