package ma.emsi.userservice.repository;

import ma.emsi.userservice.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import ma.emsi.userservice.entity.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordRestTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    @Modifying
    @Transactional
    void deleteByUser(User user);

    @Modifying
    @Transactional
    void deleteByExpirationDateBefore(LocalDateTime now);
}
