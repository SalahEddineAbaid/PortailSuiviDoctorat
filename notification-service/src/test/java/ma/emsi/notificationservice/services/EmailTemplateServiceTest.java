package ma.emsi.notificationservice.services;

import ma.emsi.notificationservice.enums.TypeNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EmailTemplateService.
 * Tests template loading, variable interpolation, and fallback behavior.
 */
class EmailTemplateServiceTest {
    
    private EmailTemplateService emailTemplateService;
    
    @BeforeEach
    void setUp() {
        emailTemplateService = new EmailTemplateService();
    }
    
    @Test
    void testInterpolateVariables_withValidData() {
        // Given
        String template = "<html><body>Hello {{name}}, your email is {{email}}</body></html>";
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "John Doe");
        variables.put("email", "john@example.com");
        
        // When
        String result = emailTemplateService.interpolateVariables(template, variables);
        
        // Then
        assertTrue(result.contains("Hello John Doe"));
        assertTrue(result.contains("your email is john@example.com"));
        assertFalse(result.contains("{{"));
    }
    
    @Test
    void testInterpolateVariables_withMissingVariables() {
        // Given
        String template = "<html><body>Hello {{name}}, your email is {{email}}</body></html>";
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "John Doe");
        // email is missing
        
        // When
        String result = emailTemplateService.interpolateVariables(template, variables);
        
        // Then
        assertTrue(result.contains("Hello John Doe"));
        assertTrue(result.contains("your email is "));
        assertFalse(result.contains("{{email}}"));
    }
    
    @Test
    void testInterpolateVariables_withNullVariables() {
        // Given
        String template = "<html><body>Hello {{name}}</body></html>";
        
        // When
        String result = emailTemplateService.interpolateVariables(template, null);
        
        // Then
        assertEquals("<html><body>Hello </body></html>", result);
    }
    
    @Test
    void testInterpolateVariables_withEmptyVariables() {
        // Given
        String template = "<html><body>Hello {{name}}</body></html>";
        Map<String, Object> variables = new HashMap<>();
        
        // When
        String result = emailTemplateService.interpolateVariables(template, variables);
        
        // Then
        assertEquals("<html><body>Hello </body></html>", result);
    }
    
    @Test
    void testInterpolateVariables_withNullTemplate() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "John");
        
        // When
        String result = emailTemplateService.interpolateVariables(null, variables);
        
        // Then
        assertEquals("", result);
    }
    
    @Test
    void testInterpolateVariables_withVariablesContainingSpaces() {
        // Given
        String template = "<html><body>Hello {{ name }}, email: {{ email }}</body></html>";
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "John Doe");
        variables.put("email", "john@example.com");
        
        // When
        String result = emailTemplateService.interpolateVariables(template, variables);
        
        // Then
        assertTrue(result.contains("Hello John Doe"));
        assertTrue(result.contains("email: john@example.com"));
    }
    
    @Test
    void testGetTemplateForNotificationType_inscriptionSoumiseDirecteur() {
        // When
        String templateName = emailTemplateService.getTemplateForNotificationType(
            TypeNotification.INSCRIPTION_SOUMISE_DIRECTEUR
        );
        
        // Then
        assertEquals("template_inscription_soumise_directeur.html", templateName);
    }
    
    @Test
    void testGetTemplateForNotificationType_inscriptionValideeDirecteurDoctorant() {
        // When
        String templateName = emailTemplateService.getTemplateForNotificationType(
            TypeNotification.INSCRIPTION_VALIDEE_DIRECTEUR_DOCTORANT
        );
        
        // Then
        assertEquals("template_inscription_validee_directeur_doctorant.html", templateName);
    }
    
    @Test
    void testGetTemplateForNotificationType_juryProposeAdmin() {
        // When
        String templateName = emailTemplateService.getTemplateForNotificationType(
            TypeNotification.JURY_PROPOSE_ADMIN
        );
        
        // Then
        assertEquals("template_jury_propose_admin.html", templateName);
    }
    
    @Test
    void testGetTemplateForNotificationType_genericFallback() {
        // When
        String templateName = emailTemplateService.getTemplateForNotificationType(
            TypeNotification.NOTIFICATION_GENERALE
        );
        
        // Then
        assertEquals("template_generic.html", templateName);
    }
    
    @Test
    void testGetTemplateForNotificationType_nullType() {
        // When
        String templateName = emailTemplateService.getTemplateForNotificationType(null);
        
        // Then
        assertEquals("template_generic.html", templateName);
    }
    
    @Test
    void testLoadTemplate_genericTemplate() throws IOException {
        // When
        String template = emailTemplateService.loadTemplate("template_generic.html");
        
        // Then
        assertNotNull(template);
        assertFalse(template.isEmpty());
        assertTrue(template.contains("<!DOCTYPE html>"));
        assertTrue(template.contains("Portail"));
    }
    
    @Test
    void testLoadTemplate_nonExistentTemplate() {
        // When/Then
        assertThrows(IOException.class, () -> {
            emailTemplateService.loadTemplate("non_existent_template.html");
        });
    }
    
    @Test
    void testProcessTemplate_withGenericTemplate() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("sujet", "Test Subject");
        variables.put("nomComplet", "John Doe");
        variables.put("message", "This is a test message");
        
        // When
        String result = emailTemplateService.processTemplate(
            TypeNotification.NOTIFICATION_GENERALE, 
            variables
        );
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("Test Subject"));
        assertTrue(result.contains("John Doe"));
        assertTrue(result.contains("This is a test message"));
    }
    
    @Test
    void testProcessTemplate_fallbackToGeneric() {
        // Given - using a type that doesn't have a specific template file yet
        Map<String, Object> variables = new HashMap<>();
        variables.put("sujet", "Fallback Test");
        variables.put("nomComplet", "Jane Doe");
        
        // When
        String result = emailTemplateService.processTemplate(
            TypeNotification.INSCRIPTION_SOUMISE_DIRECTEUR, 
            variables
        );
        
        // Then - should fallback to generic template
        assertNotNull(result);
        assertTrue(result.contains("Fallback Test"));
        assertTrue(result.contains("Jane Doe"));
    }
}
