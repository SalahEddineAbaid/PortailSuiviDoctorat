package ma.emsi.notificationservice.repositories;

import ma.emsi.notificationservice.entities.NotificationDLQ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for NotificationDLQ entity.
 * Provides methods for managing Dead Letter Queue entries.
 * 
 * Requirements: 11.3
 */
@Repository
public interface NotificationDLQRepository extends JpaRepository<NotificationDLQ, Long> {
    
    /**
     * Find DLQ entry by original notification ID.
     * 
     * @param notificationId the original notification ID
     * @return optional DLQ entry
     */
    Optional<NotificationDLQ> findByNotificationId(Long notificationId);
    
    /**
     * Find all DLQ entries that have not been reprocessed.
     * 
     * @return list of unprocessed DLQ entries
     */
    List<NotificationDLQ> findByRetraiteOrderByDateAjoutDlqAsc(Boolean retraite);
    
    /**
     * Find all DLQ entries for a specific destinataire.
     * 
     * @param destinataire the email address
     * @return list of DLQ entries
     */
    List<NotificationDLQ> findByDestinataire(String destinataire);
    
    /**
     * Count total DLQ entries.
     * 
     * @return total count
     */
    @Query("SELECT COUNT(d) FROM NotificationDLQ d")
    Long countTotal();
    
    /**
     * Count unprocessed DLQ entries.
     * 
     * @return count of unprocessed entries
     */
    Long countByRetraite(Boolean retraite);
}
