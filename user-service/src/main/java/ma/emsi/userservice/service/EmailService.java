package ma.emsi.userservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            String resetLink = frontendUrl + "/reset-password?token=" + token;

            String htmlContent = """
            <html>
              <body style="font-family: Arial, sans-serif; padding: 20px;">
                <h2 style="color: #4CAF50;">Réinitialisation de mot de passe</h2>
                <p>Bonjour,</p>
                <p>Cliquez sur le bouton ci-dessous pour réinitialiser votre mot de passe :</p>
                <a href="%s" style="background-color: #4CAF50; color: white; padding: 14px 20px; text-decoration: none; border-radius: 4px;">
                  Réinitialiser mon mot de passe
                </a>
                <p style="margin-top: 20px;">Ce lien est valide pendant 15 minutes.</p>
              </body>
            </html>
            """.formatted(resetLink);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Réinitialisation de votre mot de passe");
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Email de réinitialisation envoyé avec succès à {}", toEmail);

        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'email à {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Impossible d'envoyer l'email de réinitialisation", e);
        }
    }
}
