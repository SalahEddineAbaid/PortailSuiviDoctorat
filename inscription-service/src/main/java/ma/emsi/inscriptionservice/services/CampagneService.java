package ma.emsi.inscriptionservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.inscriptionservice.DTOs.CampagneRequest;
import ma.emsi.inscriptionservice.DTOs.CampagneResponse;
import ma.emsi.inscriptionservice.entities.Campagne;
import ma.emsi.inscriptionservice.enums.TypeCampagne;
import ma.emsi.inscriptionservice.repositories.CampagneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampagneService {

    private final CampagneRepository campagneRepository;

    @Transactional
    public CampagneResponse creerCampagne(CampagneRequest request) {
        log.info("Création d'une nouvelle campagne: {}", request.getLibelle());

        // Vérifier qu'il n'y a pas déjà une campagne pour cette année
        campagneRepository.findByTypeAndAnneeUniversitaire(
                request.getType(),
                request.getAnneeUniversitaire()
        ).ifPresent(c -> {
            throw new RuntimeException("Une campagne existe déjà pour cette année universitaire");
        });

        Campagne campagne = Campagne.builder()
                .libelle(request.getLibelle())
                .type(request.getType())
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .anneeUniversitaire(request.getAnneeUniversitaire())
                .active(true)
                .build();

        campagne = campagneRepository.save(campagne);

        log.info("Campagne créée: ID {}", campagne.getId());

        return mapToResponse(campagne);
    }

    public CampagneResponse getCampagne(Long id) {
        Campagne campagne = campagneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campagne introuvable"));
        return mapToResponse(campagne);
    }

    public List<CampagneResponse> getAllCampagnes() {
        return campagneRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CampagneResponse> getCampagnesActives() {
        return campagneRepository.findAll()
                .stream()
                .filter(Campagne::isOuverte)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CampagneResponse fermerCampagne(Long id) {
        log.info("Fermeture de la campagne {}", id);

        Campagne campagne = campagneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campagne introuvable"));

        campagne.fermer();
        campagne = campagneRepository.save(campagne);

        return mapToResponse(campagne);
    }

    @Transactional
    public CampagneResponse modifierCampagne(Long id, CampagneRequest request) {
        log.info("Modification de la campagne {}", id);

        Campagne campagne = campagneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campagne introuvable"));

        campagne.setLibelle(request.getLibelle());
        campagne.setDateDebut(request.getDateDebut());
        campagne.setDateFin(request.getDateFin());

        campagne = campagneRepository.save(campagne);

        return mapToResponse(campagne);
    }

    private CampagneResponse mapToResponse(Campagne campagne) {
        return CampagneResponse.builder()
                .id(campagne.getId())
                .libelle(campagne.getLibelle())
                .type(campagne.getType())
                .dateDebut(campagne.getDateDebut())
                .dateFin(campagne.getDateFin())
                .active(campagne.getActive())
                .anneeUniversitaire(campagne.getAnneeUniversitaire())
                .ouverte(campagne.isOuverte())
                .build();
    }
}

