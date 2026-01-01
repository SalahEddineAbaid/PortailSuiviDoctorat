package ma.emsi.notificationservice.repositories;

import ma.emsi.notificationservice.entities.Notification;
import ma.emsi.notificationservice.enums.StatutNotification;
import ma.emsi.notificationservice.enums.TypeNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Notification entity.
 * Provides custom query methods for notification retrieval and filtering.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

        /**
         * Find all notifications for a specific user email address.
         */
        Page<Notification> findByDestinataire(String destinataire, Pageable pageable);

        /**
         * Find all notifications with a specific status.
         */
        Page<Notification> findByStatut(StatutNotification statut, Pageable pageable);

        /**
         * Find all notifications with a specific type.
         */
        Page<Notification> findByType(TypeNotification type, Pageable pageable);

        /**
         * Find all failed notifications.
         */
        List<Notification> findByStatut(StatutNotification statut);

        /**
         * Count notifications by status.
         */
        Long countByStatut(StatutNotification statut);

        /**
         * Find notifications by multiple criteria.
         */
        @Query("SELECT n FROM Notification n WHERE " +
                        "(:destinataire IS NULL OR n.destinataire = :destinataire) AND " +
                        "(:type IS NULL OR n.type = :type) AND " +
                        "(:statut IS NULL OR n.statut = :statut) AND " +
                        "(:dateDebut IS NULL OR n.dateCreation >= :dateDebut) AND " +
                        "(:dateFin IS NULL OR n.dateCreation <= :dateFin)")
        Page<Notification> searchNotifications(
                        @Param("destinataire") String destinataire,
                        @Param("type") TypeNotification type,
                        @Param("statut") StatutNotification statut,
                        @Param("dateDebut") LocalDateTime dateDebut,
                        @Param("dateFin") LocalDateTime dateFin,
                        Pageable pageable);

        /**
         * Get statistics for all notifications.
         */
        @Query("SELECT COUNT(n) FROM Notification n")
        Long countTotal();

        /**
         * Find notifications that need retry (FAILED status with retry count < max).
         */
        @Query("SELECT n FROM Notification n WHERE n.statut = :statut AND n.nombreTentatives < :maxRetries")
        List<Notification> findRetryableNotifications(
                        @Param("statut") StatutNotification statut,
                        @Param("maxRetries") Integer maxRetries);

        /**
         * Find unread notifications for a specific user ID.
         */
        List<Notification> findByUserIdAndLuFalse(Long userId);

        /**
         * Find all notifications for a specific user ID.
         */
        List<Notification> findByUserId(Long userId);
}
