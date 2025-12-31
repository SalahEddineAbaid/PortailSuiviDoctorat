package ma.emsi.notificationservice.services;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for sending emails with resilience patterns.
 * Implements circuit breaker, retry, timeout, and bulkhead patterns
 * to ensure reliable email delivery even when SMTP server is unstable.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {
    
    private final JavaMailSender javaMailSender;
    private final MetricsService metricsService;
    
    @Value("${notification.email.from}")
    private String fromAddress;
    
    /**
     * Sends an HTML email with resilience patterns applied.
     * 
     * Requirement 13.1: WHEN a notification is successfully sent THEN log at INFO level
     * Requirement 13.2: WHEN a notification fails THEN log at ERROR level
     * Requirement 13.3: WHEN a retry is attempted THEN log at INFO level
     * 
     * @param to recipient email address
     * @param subject email subject
     * @param htmlContent HTML content of the email
     * @throws MessagingException if email sending fails
     */
    @CircuitBreaker(name = "emailService", fallbackMethod = "fallbackSendEmail")
    @Retry(name = "emailService")
    @TimeLimiter(name = "emailService")
    @Bulkhead(name = "emailService")
    public CompletableFuture<Void> sendEmail(String to, String subject, String htmlContent) throws MessagingException {
        return CompletableFuture.runAsync(() -> {
            Instant startTime = Instant.now();
            try {
                log.debug("Attempting to send email to: {}, subject: {}", to, subject);
                
                MimeMessage message = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                
                helper.setFrom(fromAddress);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlContent, true); // true = HTML content
                
                javaMailSender.send(message);
                
                // Record email send duration (Requirement 13.7)
                Duration duration = Duration.between(startTime, Instant.now());
                metricsService.recordEmailSendDuration(duration);
                
                // Requirement 13.1: Log successful email send at INFO level
                log.info("Email sent successfully to: {}, subject: {}, duration: {} ms", 
                         to, subject, duration.toMillis());
            } catch (MessagingException e) {
                // Requirement 13.2: Log failed email send at ERROR level
                log.error("Failed to send email to: {}, subject: {}, error: {}", 
                          to, subject, e.getMessage(), e);
                throw new RuntimeException("Email send failed", e);
            }
        });
    }
    
    /**
     * Sends an HTML email with attachment.
     * 
     * @param to recipient email address
     * @param subject email subject
     * @param htmlContent HTML content of the email
     * @param attachmentName name of the attachment file
     * @param attachmentData byte array of the attachment
     * @throws MessagingException if email sending fails
     */
    @CircuitBreaker(name = "emailService", fallbackMethod = "fallbackSendEmailWithAttachment")
    @Retry(name = "emailService")
    @TimeLimiter(name = "emailService")
    @Bulkhead(name = "emailService")
    public CompletableFuture<Void> sendEmailWithAttachment(
            String to, 
            String subject, 
            String htmlContent,
            String attachmentName,
            byte[] attachmentData) throws MessagingException {
        
        return CompletableFuture.runAsync(() -> {
            Instant startTime = Instant.now();
            try {
                log.debug("Attempting to send email with attachment to: {}, subject: {}, attachment: {}", 
                         to, subject, attachmentName);
                
                MimeMessage message = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                
                helper.setFrom(fromAddress);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlContent, true); // true = HTML content
                
                // Add attachment
                if (attachmentData != null && attachmentName != null) {
                    helper.addAttachment(attachmentName, new ByteArrayResource(attachmentData));
                }
                
                javaMailSender.send(message);
                
                // Record email send duration (Requirement 13.7)
                Duration duration = Duration.between(startTime, Instant.now());
                metricsService.recordEmailSendDuration(duration);
                
                log.info("Email with attachment sent successfully to: {}, subject: {}, attachment: {}", 
                        to, subject, attachmentName);
            } catch (MessagingException e) {
                log.error("Failed to send email with attachment to: {}, subject: {}", to, subject, e);
                throw new RuntimeException("Email send with attachment failed", e);
            }
        });
    }
    
    /**
     * Fallback method for sendEmail when circuit breaker opens or all retries fail.
     * Logs the failure and returns a completed future with exception.
     * 
     * @param to recipient email address
     * @param subject email subject
     * @param htmlContent HTML content
     * @param throwable the exception that triggered the fallback
     * @return CompletableFuture that completes exceptionally
     */
    public CompletableFuture<Void> fallbackSendEmail(
            String to, 
            String subject, 
            String htmlContent, 
            Throwable throwable) {
        
        log.error("Fallback triggered for email to: {}, subject: {}. Reason: {}", 
                 to, subject, throwable.getMessage());
        
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.completeExceptionally(
            new MessagingException("Email service unavailable: " + throwable.getMessage())
        );
        return future;
    }
    
    /**
     * Fallback method for sendEmailWithAttachment when circuit breaker opens or all retries fail.
     * Logs the failure and returns a completed future with exception.
     * 
     * @param to recipient email address
     * @param subject email subject
     * @param htmlContent HTML content
     * @param attachmentName attachment filename
     * @param attachmentData attachment data
     * @param throwable the exception that triggered the fallback
     * @return CompletableFuture that completes exceptionally
     */
    public CompletableFuture<Void> fallbackSendEmailWithAttachment(
            String to, 
            String subject, 
            String htmlContent,
            String attachmentName,
            byte[] attachmentData,
            Throwable throwable) {
        
        log.error("Fallback triggered for email with attachment to: {}, subject: {}, attachment: {}. Reason: {}", 
                 to, subject, attachmentName, throwable.getMessage());
        
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.completeExceptionally(
            new MessagingException("Email service unavailable: " + throwable.getMessage())
        );
        return future;
    }
}
