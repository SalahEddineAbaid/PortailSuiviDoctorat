package ma.emsi.inscriptionservice.repositories;

import ma.emsi.inscriptionservice.entities.Campagne;
import ma.emsi.inscriptionservice.enums.TypeCampagne;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampagneRepository extends JpaRepository<Campagne, Long> {

    List<Campagne> findByTypeAndActive(TypeCampagne type, Boolean active);

    @Query("SELECT c FROM Campagne c WHERE c.type = :type AND c.active = true " +
            "AND :date BETWEEN c.dateDebut AND c.dateFin")
    Optional<Campagne> findCampagneOuverte(TypeCampagne type, LocalDate date);

    Optional<Campagne> findByTypeAndAnneeUniversitaire(TypeCampagne type, Integer anneeUniversitaire);

    List<Campagne> findByAnneeUniversitaire(Integer anneeUniversitaire);
    
    List<Campagne> findByDateDebut(LocalDate dateDebut);
    
    List<Campagne> findByDateFin(LocalDate dateFin);
}
