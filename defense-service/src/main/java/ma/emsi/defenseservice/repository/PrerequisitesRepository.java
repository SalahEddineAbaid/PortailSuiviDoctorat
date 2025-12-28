package ma.emsi.defenseservice.repository;

import ma.emsi.defenseservice.entity.Prerequisites;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrerequisitesRepository extends JpaRepository<Prerequisites, Long> {

    /**
     * Trouver tous les prérequis d'un doctorant
     */
    List<Prerequisites> findByDoctorantId(Long doctorantId);

    /**
     * Trouver les prérequis validés d'un doctorant
     */
    List<Prerequisites> findByDoctorantIdAndIsValid(Long doctorantId, boolean isValid);
}
