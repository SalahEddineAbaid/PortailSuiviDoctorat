package ma.emsi.defenseservice.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import ma.emsi.defenseservice.dto.external.UserDTO;
import ma.emsi.defenseservice.entity.Defense;
import ma.emsi.defenseservice.entity.JuryMember;
import ma.emsi.defenseservice.enums.MemberRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating Procès-Verbal (defense minutes) PDF documents
 * Implements Requirements 3.2, 3.3
 */
@Service
public class ProcesVerbalPdfGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ProcesVerbalPdfGenerator.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    @Autowired
    private UserServiceFacade userServiceFacade;

    /**
     * Generate a Procès-Verbal PDF for a completed defense
     * 
     * @param defense The finalized defense entity
     * @return PDF document as byte array
     */
    public byte[] generateProcesVerbal(Defense defense) {
        logger.info("📄 Generating Procès-Verbal for defense ID: {}", defense.getId());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(50, 50, 50, 50);

            // Get doctorant information
            UserDTO doctorant = userServiceFacade.getUserById(
                    defense.getDefenseRequest().getDoctorantId());

            // Add document sections
            addHeader(document);
            addDoctorantInformation(document, doctorant);
            addThesisTitle(document, defense);
            addDefenseDetails(document, defense);
            addJuryComposition(document, defense);
            addDefenseOutcome(document, defense);
            addJuryRecommendations(document, defense);
            addSignatureSpaces(document, defense);
            addFooter(document, defense);

            document.close();
            logger.info("✅ Procès-Verbal generated successfully for defense ID: {}", defense.getId());

            return baos.toByteArray();
        } catch (Exception e) {
            logger.error("❌ Error generating Procès-Verbal for defense ID: {}", defense.getId(), e);
            throw new RuntimeException("Failed to generate Procès-Verbal PDF", e);
        }
    }

    /**
     * Add institution header to the document
     */
    private void addHeader(Document document) {
        // Institution header
        Paragraph header = new Paragraph("UNIVERSITÉ [NOM DE L'UNIVERSITÉ]")
                .setFontSize(16)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(header);

        Paragraph subHeader = new Paragraph("École Doctorale")
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(subHeader);

        // Document title
        Paragraph title = new Paragraph("PROCÈS-VERBAL DE SOUTENANCE DE THÈSE")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30)
                .setFontColor(new DeviceRgb(0, 51, 102));
        document.add(title);
    }

    /**
     * Add doctorant information section
     */
    private void addDoctorantInformation(Document document, UserDTO doctorant) {
        Paragraph sectionTitle = new Paragraph("I. INFORMATIONS SUR LE DOCTORANT")
                .setFontSize(14)
                .setBold()
                .setMarginBottom(10)
                .setFontColor(new DeviceRgb(0, 51, 102));
        document.add(sectionTitle);

        Table table = new Table(UnitValue.createPercentArray(new float[] { 30, 70 }))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        addInfoRow(table, "Nom complet:",
                doctorant.getFirstName() + " " + doctorant.getLastName());
        addInfoRow(table, "Email:", doctorant.getEmail());
        addInfoRow(table, "Téléphone:", doctorant.getPhoneNumber() != null ? doctorant.getPhoneNumber() : "N/A");

        document.add(table);
    }

    /**
     * Add thesis title section
     */
    private void addThesisTitle(Document document, Defense defense) {
        Paragraph sectionTitle = new Paragraph("II. TITRE DE LA THÈSE")
                .setFontSize(14)
                .setBold()
                .setMarginBottom(10)
                .setFontColor(new DeviceRgb(0, 51, 102));
        document.add(sectionTitle);

        // Note: Thesis title should be added to Prerequisites or DefenseRequest entity
        // For now, using a placeholder
        Paragraph thesisTitle = new Paragraph("[Titre de la thèse à définir dans le modèle de données]")
                .setFontSize(12)
                .setItalic()
                .setMarginBottom(20);
        document.add(thesisTitle);
    }

    /**
     * Add defense date, time, and location details
     */
    private void addDefenseDetails(Document document, Defense defense) {
        Paragraph sectionTitle = new Paragraph("III. DÉTAILS DE LA SOUTENANCE")
                .setFontSize(14)
                .setBold()
                .setMarginBottom(10)
                .setFontColor(new DeviceRgb(0, 51, 102));
        document.add(sectionTitle);

        Table table = new Table(UnitValue.createPercentArray(new float[] { 30, 70 }))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        addInfoRow(table, "Date et heure:",
                defense.getDefenseDate().format(DATETIME_FORMATTER));
        addInfoRow(table, "Lieu:", defense.getLocation());
        addInfoRow(table, "Salle:", defense.getRoom());

        document.add(table);
    }

    /**
     * Add jury composition table with roles
     */
    private void addJuryComposition(Document document, Defense defense) {
        Paragraph sectionTitle = new Paragraph("IV. COMPOSITION DU JURY")
                .setFontSize(14)
                .setBold()
                .setMarginBottom(10)
                .setFontColor(new DeviceRgb(0, 51, 102));
        document.add(sectionTitle);

        Table table = new Table(UnitValue.createPercentArray(new float[] { 10, 45, 45 }))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // Header row
        table.addHeaderCell(createHeaderCell("N°"));
        table.addHeaderCell(createHeaderCell("Nom et Prénom"));
        table.addHeaderCell(createHeaderCell("Qualité"));

        // Get jury members sorted by role priority
        List<JuryMember> sortedMembers = defense.getDefenseRequest().getJury().getMembers()
                .stream()
                .sorted(Comparator.comparing(this::getRolePriority))
                .collect(Collectors.toList());

        int index = 1;
        for (JuryMember member : sortedMembers) {
            UserDTO professor = userServiceFacade.getUserById(member.getProfessorId());

            table.addCell(createCell(String.valueOf(index++)));
            table.addCell(createCell(professor.getFirstName() + " " + professor.getLastName()));
            table.addCell(createCell(getRoleLabel(member.getRole())));
        }

        document.add(table);
    }

    /**
     * Add defense outcome and mention section
     */
    private void addDefenseOutcome(Document document, Defense defense) {
        Paragraph sectionTitle = new Paragraph("V. RÉSULTAT DE LA SOUTENANCE")
                .setFontSize(14)
                .setBold()
                .setMarginBottom(10)
                .setFontColor(new DeviceRgb(0, 51, 102));
        document.add(sectionTitle);

        Table table = new Table(UnitValue.createPercentArray(new float[] { 30, 70 }))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        addInfoRow(table, "Mention obtenue:", getMentionLabel(defense.getMention()));
        addInfoRow(table, "Recommandation de publication:",
                defense.isPublicationRecommended() ? "Oui" : "Non");
        addInfoRow(table, "Date de délibération:",
                defense.getDeliberationDate().format(DATE_FORMATTER));

        document.add(table);
    }

    /**
     * Add jury recommendations section
     */
    private void addJuryRecommendations(Document document, Defense defense) {
        Paragraph sectionTitle = new Paragraph("VI. OBSERVATIONS ET RECOMMANDATIONS DU JURY")
                .setFontSize(14)
                .setBold()
                .setMarginBottom(10)
                .setFontColor(new DeviceRgb(0, 51, 102));
        document.add(sectionTitle);

        String comments = defense.getJuryComments() != null && !defense.getJuryComments().isEmpty()
                ? defense.getJuryComments()
                : "Aucune observation particulière.";

        Paragraph recommendations = new Paragraph(comments)
                .setFontSize(11)
                .setMarginBottom(30);
        document.add(recommendations);
    }

    /**
     * Add signature spaces for jury members
     */
    private void addSignatureSpaces(Document document, Defense defense) {
        Paragraph sectionTitle = new Paragraph("VII. SIGNATURES DES MEMBRES DU JURY")
                .setFontSize(14)
                .setBold()
                .setMarginBottom(20)
                .setFontColor(new DeviceRgb(0, 51, 102));
        document.add(sectionTitle);

        List<JuryMember> sortedMembers = defense.getDefenseRequest().getJury().getMembers()
                .stream()
                .sorted(Comparator.comparing(this::getRolePriority))
                .collect(Collectors.toList());

        for (JuryMember member : sortedMembers) {
            UserDTO professor = userServiceFacade.getUserById(member.getProfessorId());

            Paragraph signature = new Paragraph()
                    .add(getRoleLabel(member.getRole()) + " : ")
                    .add(professor.getFirstName() + " " + professor.getLastName())
                    .setMarginBottom(5);
            document.add(signature);

            Paragraph signatureLine = new Paragraph("Signature : _______________________")
                    .setMarginBottom(20);
            document.add(signatureLine);
        }
    }

    /**
     * Add document footer
     */
    private void addFooter(Document document, Defense defense) {
        Paragraph footer = new Paragraph()
                .add("Fait à " + defense.getLocation() + ", ")
                .add("le " + defense.getDeliberationDate().format(DATE_FORMATTER))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(30);
        document.add(footer);
    }

    // Helper methods

    private void addInfoRow(Table table, String label, String value) {
        table.addCell(createCell(label).setBold());
        table.addCell(createCell(value));
    }

    private Cell createHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold())
                .setBackgroundColor(new DeviceRgb(0, 51, 102))
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(8);
    }

    private Cell createCell(String text) {
        return new Cell()
                .add(new Paragraph(text))
                .setPadding(5)
                .setBorder(Border.NO_BORDER);
    }

    private int getRolePriority(JuryMember member) {
        return switch (member.getRole()) {
            case PRESIDENT -> 1;
            case DIRECTEUR_THESE -> 2;
            case CO_DIRECTEUR -> 3;
            case RAPPORTEUR -> 4;
            case EXAMINATEUR -> 5;
        };
    }

    private String getRoleLabel(MemberRole role) {
        return switch (role) {
            case PRESIDENT -> "Président du Jury";
            case DIRECTEUR_THESE -> "Directeur de Thèse";
            case CO_DIRECTEUR -> "Co-Directeur de Thèse";
            case RAPPORTEUR -> "Rapporteur";
            case EXAMINATEUR -> "Examinateur";
        };
    }

    private String getMentionLabel(ma.emsi.defenseservice.enums.Mention mention) {
        return switch (mention) {
            case TRES_HONORABLE_AVEC_FELICITATIONS -> "Très Honorable avec Félicitations du Jury";
            case TRES_HONORABLE -> "Très Honorable";
            case HONORABLE -> "Honorable";
            case PASSABLE -> "Passable";
        };
    }
}
