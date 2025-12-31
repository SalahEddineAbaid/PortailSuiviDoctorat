package ma.emsi.notificationservice.services;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailService.
 * Tests basic email sending functionality with mocked JavaMailSender.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
    
    @Mock
    private JavaMailSender javaMailSender;
    
    @Mock
    private MetricsService metricsService;
    
    @InjectMocks
    private EmailService emailService;
    
    private static final String FROM_ADDRESS = "noreply@portail-doctorat.ma";
    private static final String TO_ADDRESS = "test@example.com";
    private static final String SUBJECT = "Test Subject";
    private static final String HTML_CONTENT = "<html><body>Test Content</body></html>";
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromAddress", FROM_ADDRESS);
    }
    
    @Test
    void testSendEmail_Success() throws Exception {
        // Arrange
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(javaMailSender).send(any(MimeMessage.class));
        
        // Act
        CompletableFuture<Void> result = emailService.sendEmail(TO_ADDRESS, SUBJECT, HTML_CONTENT);
        result.get(); // Wait for completion
        
        // Assert
        verify(javaMailSender, times(1)).createMimeMessage();
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }
    
    @Test
    void testSendEmail_Failure() throws Exception {
        // Arrange
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP error")).when(javaMailSender).send(any(MimeMessage.class));
        
        // Act
        CompletableFuture<Void> result = emailService.sendEmail(TO_ADDRESS, SUBJECT, HTML_CONTENT);
        
        // Assert
        assertThrows(ExecutionException.class, result::get);
    }
    
    @Test
    void testSendEmailWithAttachment_Success() throws Exception {
        // Arrange
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(javaMailSender).send(any(MimeMessage.class));
        
        byte[] attachmentData = "test data".getBytes();
        String attachmentName = "test.pdf";
        
        // Act
        CompletableFuture<Void> result = emailService.sendEmailWithAttachment(
            TO_ADDRESS, SUBJECT, HTML_CONTENT, attachmentName, attachmentData
        );
        result.get(); // Wait for completion
        
        // Assert
        verify(javaMailSender, times(1)).createMimeMessage();
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }
    
    @Test
    void testFallbackSendEmail() {
        // Arrange
        Throwable throwable = new RuntimeException("Service unavailable");
        
        // Act
        CompletableFuture<Void> result = emailService.fallbackSendEmail(
            TO_ADDRESS, SUBJECT, HTML_CONTENT, throwable
        );
        
        // Assert
        assertTrue(result.isCompletedExceptionally());
        assertThrows(ExecutionException.class, result::get);
    }
    
    @Test
    void testFallbackSendEmailWithAttachment() {
        // Arrange
        Throwable throwable = new RuntimeException("Service unavailable");
        byte[] attachmentData = "test data".getBytes();
        String attachmentName = "test.pdf";
        
        // Act
        CompletableFuture<Void> result = emailService.fallbackSendEmailWithAttachment(
            TO_ADDRESS, SUBJECT, HTML_CONTENT, attachmentName, attachmentData, throwable
        );
        
        // Assert
        assertTrue(result.isCompletedExceptionally());
        assertThrows(ExecutionException.class, result::get);
    }
}
