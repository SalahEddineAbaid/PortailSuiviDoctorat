package ma.emsi.userservice.repository;

import ma.emsi.userservice.entity.User;
import ma.emsi.userservice.enums.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findByAccountStatus(AccountStatus status, Pageable pageable);

    long countByAccountStatus(AccountStatus status);

    long countByCreatedAtAfter(LocalDateTime date);
}
