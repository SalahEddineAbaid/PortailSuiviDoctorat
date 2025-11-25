package ma.emsi.defenseservice.repository;

import ma.emsi.defenseservice.entity.DefenseRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DefenseRequestRepository extends JpaRepository<DefenseRequest, Long> {

}
