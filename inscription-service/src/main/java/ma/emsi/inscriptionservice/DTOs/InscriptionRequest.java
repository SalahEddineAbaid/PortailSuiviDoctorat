package ma.emsi.inscriptionservice.DTOs;

import jakarta.validation.constraints.*;
import lombok.Data;
import ma.emsi.inscriptionservice.enums.TypeInscription;

import java.time.LocalDate;

@Data
public class InscriptionRequest {

    @NotNull(message = "L'identifiant du doctorant est requis")
    private Long doctorantId;

    @NotNull(message = "L'identifiant du directeur de thèse est requis")
    private Long directeurTheseId;

    @NotNull(message = "L'identifiant de la campagne est requis")
    private Long campagneId;

    @NotBlank(message = "Le sujet de thèse est requis")
    @Size(max = 500, message = "Le sujet ne doit pas dépasser 500 caractères")
    private String sujetThese;

    @NotNull(message = "Le type d'inscription est requis")
    private TypeInscription type;

    @NotNull(message = "L'année d'inscription est requise")
    private Integer anneeInscription;

    // Informations doctorant
    @NotBlank(message = "Le CIN est requis")
    private String cin;

    private String cne;

    @NotBlank(message = "Le téléphone est requis")
    @Pattern(regexp = "^[0-9]{10}$", message = "Le téléphone doit contenir 10 chiffres")
    private String telephone;

    @NotBlank(message = "L'adresse est requise")
    private String adresse;

    @NotBlank(message = "La ville est requise")
    private String ville;

    @NotBlank(message = "Le pays est requis")
    private String pays;

    @NotNull(message = "La date de naissance est requise")
    @Past(message = "La date de naissance doit être dans le passé")
    private LocalDate dateNaissance;

    @NotBlank(message = "Le lieu de naissance est requis")
    private String lieuNaissance;

    @NotBlank(message = "La nationalité est requise")
    private String nationalite;

    // Informations thèse
    @NotBlank(message = "Le titre de la thèse est requis")
    @Size(max = 500, message = "Le titre ne doit pas dépasser 500 caractères")
    private String titreThese;

    @NotBlank(message = "La discipline est requise")
    private String discipline;

    @NotBlank(message = "Le laboratoire est requis")
    private String laboratoire;

    @NotBlank(message = "L'établissement d'accueil est requis")
    private String etablissementAccueil;

    private Boolean cotutelle = false;

    private String universitePartenaire;

    private String paysPartenaire;

    @NotNull(message = "La date de début prévue est requise")
    @Future(message = "La date de début doit être dans le futur")
    private LocalDate dateDebutPrevue;
}
