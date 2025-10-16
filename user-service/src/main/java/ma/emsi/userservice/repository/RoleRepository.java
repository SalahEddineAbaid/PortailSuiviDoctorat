package ma.emsi.userservice.repository;

import ma.emsi.userservice.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import ma.emsi.userservice.entity.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);

}
