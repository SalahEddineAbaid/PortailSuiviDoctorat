package ma.emsi.defenseservice.repository;

import jdk.jfr.Registered;
import ma.emsi.defenseservice.entity.Prerequisites;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrerequisitesRepository extends JpaRepository<Prerequisites, Long> {

}
