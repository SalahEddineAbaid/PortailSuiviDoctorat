package ma.emsi.inscriptionservice.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private DoctorantInfo doctorant;
    private InscriptionCourante inscriptionCourante;
    private List<InscriptionHistorique> historiqueInscriptions;
    private List<AlerteInfo> alertes;
    private List<DocumentManquant> documentsManquants;
    private Milestone prochaineMilestone;
    private StatistiquesDossier statistiques;
}
