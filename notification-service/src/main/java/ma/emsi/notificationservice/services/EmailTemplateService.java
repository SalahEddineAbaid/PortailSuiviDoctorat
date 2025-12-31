package ma.emsi.notificationservice.services;

import lombok.extern.slf4j.Slf4j;
import ma.emsi.notificationservice.enums.TypeNotification;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service responsible for loading and processing HTML email templates.
 * Handles template selection based on notification type and variable interpolation.
 */
@Service
@Slf4j
public class EmailTemplateService {
    
    private static final String TEMPLATES_PATH = "templates/emails/";
    private static final String GENERIC_TEMPLATE = "template_generic.html";
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    
    /**
     * Loads an HTML template file from the resources directory.
     * 
     * @param templateName the name of the template file
     * @return the template content as a string
     * @throws IOException if the template file cannot be read
     */
    public String loadTemplate(String templateName) throws IOException {
        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATES_PATH + templateName);
            byte[] bytes = Files.readAllBytes(resource.getFile().toPath());
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load template: {}", templateName, e);
            throw e;
        }
    }
    
    /**
     * Interpolates variables in a template by replacing {{variable}} placeholders
     * with values from the provided data map.
     * Missing variables are replaced with empty strings.
     * 
     * @param template the template content with {{variable}} placeholders
     * @param variables the map of variable names to values
     * @return the template with all variables replaced
     */
    public String interpolateVariables(String template, Map<String, Object> variables) {
        if (template == null) {
            return "";
        }
        
        if (variables == null || variables.isEmpty()) {
            // Replace all variables with empty string if no data provided
            return VARIABLE_PATTERN.matcher(template).replaceAll("");
        }
        
        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        
        while (matcher.find()) {
            String variableName = matcher.group(1).trim();
            Object value = variables.get(variableName);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Gets the appropriate template filename for a given notification type.
     * Returns the generic template name if no specific template is mapped.
     * 
     * @param type the notification type
     * @return the template filename
     */
    public String getTemplateForNotificationType(TypeNotification type) {
        if (type == null) {
            return GENERIC_TEMPLATE;
        }
        
        switch (type) {
            // Inscription workflow templates
            case INSCRIPTION_SOUMISE_DIRECTEUR:
                return "template_inscription_soumise_directeur.html";
            case INSCRIPTION_VALIDEE_DIRECTEUR_DOCTORANT:
                return "template_inscription_validee_directeur_doctorant.html";
            case INSCRIPTION_VALIDEE_DIRECTEUR_ADMIN:
                return "template_inscription_validee_directeur_admin.html";
            case INSCRIPTION_REJETEE_DIRECTEUR:
                return "template_inscription_rejetee_directeur.html";
            case INSCRIPTION_VALIDEE_ADMIN:
                return "template_inscription_validee_admin.html";
            case INSCRIPTION_REJETEE_ADMIN:
                return "template_inscription_rejetee_admin.html";
            case DEROGATION_DEMANDEE:
                return "template_derogation_demandee.html";
            
            // Defense workflow templates
            case DEMANDE_SOUTENANCE_SOUMISE_DIRECTEUR:
                return "template_demande_soutenance_soumise_directeur.html";
            case JURY_PROPOSE_ADMIN:
                return "template_jury_propose_admin.html";
            case JURY_MEMBRE_INVITE:
                return "template_jury_membre_invite.html";
            case JURY_MEMBRE_ACCEPTE_DIRECTEUR:
                return "template_jury_membre_accepte_directeur.html";
            case JURY_MEMBRE_DECLINE_DIRECTEUR:
                return "template_jury_membre_decline_directeur.html";
            case RAPPORT_SOUMIS_DIRECTEUR:
                return "template_rapport_soumis_directeur.html";
            case AUTORISATION_SOUTENANCE_DOCTORANT:
                return "template_autorisation_soutenance_doctorant.html";
            case SOUTENANCE_PLANIFIEE_TOUS:
                return "template_soutenance_planifiee_tous.html";
            
            // Generic fallback for other types
            default:
                return GENERIC_TEMPLATE;
        }
    }
    
    /**
     * Loads and processes a template for a given notification type.
     * Falls back to the generic template if the specific template is not found.
     * 
     * @param type the notification type
     * @param variables the variables to interpolate
     * @return the processed template content
     */
    public String processTemplate(TypeNotification type, Map<String, Object> variables) {
        String templateName = getTemplateForNotificationType(type);
        
        try {
            String template = loadTemplate(templateName);
            return interpolateVariables(template, variables);
        } catch (IOException e) {
            log.warn("Template {} not found, falling back to generic template", templateName);
            try {
                String genericTemplate = loadTemplate(GENERIC_TEMPLATE);
                return interpolateVariables(genericTemplate, variables);
            } catch (IOException ex) {
                log.error("Failed to load generic template", ex);
                return "Error loading email template";
            }
        }
    }
}
