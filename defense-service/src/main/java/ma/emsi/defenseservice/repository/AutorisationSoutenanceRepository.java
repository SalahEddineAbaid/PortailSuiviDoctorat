package ma.emsi.defenseservice.repository;

import ma.emsi.defenseservice.entity.AutorisationSoutenance;
import ma.emsi.defenseservice.enums.StatutAutorisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AutorisationSoutenanceRepository extends JpaRepository<AutorisationSoutenance, Long> {

    Optional<AutorisationSoutenance> findByDefenseRequestId(Long defenseRequestId);

    List<AutorisationSoutenance> findByStatut(StatutAutorisation statut);

    boolean existsByDefenseRequestId(Long defenseRequestId);
}
