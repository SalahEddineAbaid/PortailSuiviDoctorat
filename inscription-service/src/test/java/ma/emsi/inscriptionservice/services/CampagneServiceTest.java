package ma.emsi.inscriptionservice.services;

import ma.emsi.inscriptionservice.DTOs.CampagneResponse;
import ma.emsi.inscriptionservice.entities.Campagne;
import ma.emsi.inscriptionservice.enums.TypeCampagne;
import ma.emsi.inscriptionservice.repositories.CampagneRepository;
import ma.emsi.inscriptionservice.repositories.InscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampagneServiceTest {

    @Mock
    private CampagneRepository campagneRepository;

    @Mock
    private InscriptionRepository inscriptionRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private CampagneService campagneService;

    private Campagne campagneSource;

    @BeforeEach
    void setUp() {
        campagneSource = Campagne.builder()
                .id(1L)
                .libelle("Inscription Doctorat 2024")
                .type(TypeCampagne.INSCRIPTION)
                .dateDebut(LocalDate.of(2024, 9, 1))
                .dateFin(LocalDate.of(2024, 10, 31))
                .anneeUniversitaire(2024)
                .active(true)
                .build();
    }

    @Test
    void testClonerCampagne_Success() {
        // Given
        Long campagneId = 1L;
        LocalDate nouvelleDateDebut = LocalDate.of(2025, 9, 1);
        LocalDate nouvelleDateFin = LocalDate.of(2025, 10, 31);

        when(campagneRepository.findById(campagneId)).thenReturn(Optional.of(campagneSource));
        
        Campagne campagneClonee = Campagne.builder()
                .id(2L)
                .libelle("Inscription Doctorat 2025")
                .type(TypeCampagne.INSCRIPTION)
                .dateDebut(nouvelleDateDebut)
                .dateFin(nouvelleDateFin)
                .anneeUniversitaire(2025)
                .active(false)
                .build();
        
        when(campagneRepository.save(any(Campagne.class))).thenReturn(campagneClonee);

        // When
        CampagneResponse result = campagneService.clonerCampagne(campagneId, nouvelleDateDebut, nouvelleDateFin);

        // Then
        assertNotNull(result);
        assertEquals("Inscription Doctorat 2025", result.getLibelle());
        assertEquals(TypeCampagne.INSCRIPTION, result.getType());
        assertEquals(nouvelleDateDebut, result.getDateDebut());
        assertEquals(nouvelleDateFin, result.getDateFin());
        assertEquals(2025, result.getAnneeUniversitaire());
        assertFalse(result.getActive());

        verify(campagneRepository).findById(campagneId);
        verify(campagneRepository).save(any(Campagne.class));
    }

    @Test
    void testClonerCampagne_IncrementYearInLibelle() {
        // Given
        Long campagneId = 1L;
        LocalDate nouvelleDateDebut = LocalDate.of(2025, 9, 1);
        LocalDate nouvelleDateFin = LocalDate.of(2025, 10, 31);

        when(campagneRepository.findById(campagneId)).thenReturn(Optional.of(campagneSource));
        
        when(campagneRepository.save(any(Campagne.class))).thenAnswer(invocation -> {
            Campagne saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        // When
        CampagneResponse result = campagneService.clonerCampagne(campagneId, nouvelleDateDebut, nouvelleDateFin);

        // Then
        assertNotNull(result);
        assertTrue(result.getLibelle().contains("2025"), 
                "Le libellé devrait contenir l'année incrémentée: " + result.getLibelle());
        
        verify(campagneRepository).save(argThat(campagne -> 
            campagne.getLibelle().contains("2025") && 
            !campagne.getActive()
        ));
    }

    @Test
    void testClonerCampagne_CampagneNotFound() {
        // Given
        Long campagneId = 999L;
        LocalDate nouvelleDateDebut = LocalDate.of(2025, 9, 1);
        LocalDate nouvelleDateFin = LocalDate.of(2025, 10, 31);

        when(campagneRepository.findById(campagneId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            campagneService.clonerCampagne(campagneId, nouvelleDateDebut, nouvelleDateFin)
        );

        verify(campagneRepository).findById(campagneId);
        verify(campagneRepository, never()).save(any());
    }

    @Test
    void testClonerCampagne_LibelleWithoutYear() {
        // Given
        Campagne campagneSansAnnee = Campagne.builder()
                .id(1L)
                .libelle("Inscription Doctorat")
                .type(TypeCampagne.INSCRIPTION)
                .dateDebut(LocalDate.of(2024, 9, 1))
                .dateFin(LocalDate.of(2024, 10, 31))
                .anneeUniversitaire(2024)
                .active(true)
                .build();

        Long campagneId = 1L;
        LocalDate nouvelleDateDebut = LocalDate.of(2025, 9, 1);
        LocalDate nouvelleDateFin = LocalDate.of(2025, 10, 31);

        when(campagneRepository.findById(campagneId)).thenReturn(Optional.of(campagneSansAnnee));
        
        when(campagneRepository.save(any(Campagne.class))).thenAnswer(invocation -> {
            Campagne saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        // When
        CampagneResponse result = campagneService.clonerCampagne(campagneId, nouvelleDateDebut, nouvelleDateFin);

        // Then
        assertNotNull(result);
        assertTrue(result.getLibelle().matches(".*\\d{4}.*"), 
                "Le libellé devrait contenir une année: " + result.getLibelle());
    }

    @Test
    void testClonerCampagne_PreservesType() {
        // Given
        Campagne campagneReinscription = Campagne.builder()
                .id(1L)
                .libelle("Réinscription Doctorat 2024")
                .type(TypeCampagne.REINSCRIPTION)
                .dateDebut(LocalDate.of(2024, 9, 1))
                .dateFin(LocalDate.of(2024, 10, 31))
                .anneeUniversitaire(2024)
                .active(true)
                .build();

        Long campagneId = 1L;
        LocalDate nouvelleDateDebut = LocalDate.of(2025, 9, 1);
        LocalDate nouvelleDateFin = LocalDate.of(2025, 10, 31);

        when(campagneRepository.findById(campagneId)).thenReturn(Optional.of(campagneReinscription));
        
        when(campagneRepository.save(any(Campagne.class))).thenAnswer(invocation -> {
            Campagne saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        // When
        CampagneResponse result = campagneService.clonerCampagne(campagneId, nouvelleDateDebut, nouvelleDateFin);

        // Then
        assertNotNull(result);
        assertEquals(TypeCampagne.REINSCRIPTION, result.getType());
        
        verify(campagneRepository).save(argThat(campagne -> 
            campagne.getType() == TypeCampagne.REINSCRIPTION
        ));
    }
}
