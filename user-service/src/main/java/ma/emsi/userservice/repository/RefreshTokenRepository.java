package ma.emsi.userservice.repository;

import ma.emsi.userservice.entity.RefreshToken;
import ma.emsi.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user);
    boolean existsByToken(String token);
}