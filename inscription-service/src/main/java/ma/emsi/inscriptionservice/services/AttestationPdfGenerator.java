package ma.emsi.inscriptionservice.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.inscriptionservice.DTOs.UserDTO;
import ma.emsi.inscriptionservice.client.UserServiceClient;
import ma.emsi.inscriptionservice.entities.DocumentGenere;
import ma.emsi.inscriptionservice.entities.InfosDoctorant;
import ma.emsi.inscriptionservice.entities.Inscription;
import ma.emsi.inscriptionservice.enums.TypeDocumentGenere;
import ma.emsi.inscriptionservice.repositories.DocumentGenereRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttestationPdfGenerator {

    private final DocumentGenereRepository documentGenereRepository;
    private final ResourceLoader resourceLoader;
    private final UserServiceClient userServiceClient;

    @Value("${pdf.logo.path}")
    private String logoPath;

    @Value("${pdf.signature.path}")
    private String signaturePath;

    @Value("${pdf.qrcode.base-url}")
    private String qrCodeBaseUrl;

    @Value("${pdf.attestation.output-dir}")
    private String outputDir;

    /**
     * Generates an attestation PDF for a validated inscription
     * 
     * @param inscription The inscription entity
     * @param infosDoctorant Student information
     * @param directeur Director information from user service
     * @return Path to the generated PDF file
     * @throws IOException if file operations fail
     */
    public String generateAttestation(Inscription inscription, InfosDoctorant infosDoctorant, UserDTO directeur) 
            throws IOException {
        
        log.info("Generating attestation for inscription ID: {}", inscription.getId());
        
        // Create output directory if it doesn't exist
        Path outputPath = Paths.get(outputDir);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }
        
        // Generate filename with timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("attestation_%d_%s.pdf", inscription.getId(), timestamp);
        String filePath = outputDir + File.separator + filename;
        
        // Create PDF
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            PdfWriter writer = new PdfWriter(fos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(50, 50, 50, 50);
            
            // Add header with logo and QR code
            addHeader(document, inscription);
            
            // Add title
            addTitle(document);
            
            // Add student information
            addStudentInformation(document, inscription, infosDoctorant);
            
            // Add thesis details
            addThesisDetails(document, inscription);
            
            // Add director information
            addDirectorInformation(document, directeur);
            
            // Add signature and stamp section
            addSignatureSection(document, inscription);
            
            document.close();
            
            log.info("Attestation generated successfully at: {}", filePath);
            
            // Create DocumentGenere record
            File file = new File(filePath);
            DocumentGenere documentGenere = DocumentGenere.builder()
                    .inscription(inscription)
                    .type(TypeDocumentGenere.ATTESTATION_INSCRIPTION)
                    .cheminFichier(filePath)
                    .tailleFichier(file.length())
                    .build();
            
            documentGenereRepository.save(documentGenere);
            log.info("DocumentGenere record created for attestation");
            
            return filePath;
            
        } catch (Exception e) {
            log.error("Error generating attestation for inscription {}: {}", inscription.getId(), e.getMessage(), e);
            throw new IOException("Failed to generate attestation PDF", e);
        }
    }

    /**
     * Adds header with institution logo and QR code
     */
    private void addHeader(Document document, Inscription inscription) throws IOException {
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        headerTable.setWidth(UnitValue.createPercentValue(100));
        
        // Add logo on the left
        try {
            Resource logoResource = resourceLoader.getResource(logoPath);
            if (logoResource.exists()) {
                Image logo = new Image(ImageDataFactory.create(logoResource.getURL()));
                logo.setWidth(100);
                logo.setHeight(100);
                headerTable.addCell(logo);
            } else {
                log.warn("Logo not found at: {}", logoPath);
                headerTable.addCell(new Paragraph("LOGO"));
            }
        } catch (Exception e) {
            log.warn("Could not load logo: {}", e.getMessage());
            headerTable.addCell(new Paragraph("LOGO"));
        }
        
        // Add QR code on the right
        try {
            byte[] qrCodeBytes = generateQRCode(inscription.getId());
            Image qrCode = new Image(ImageDataFactory.create(qrCodeBytes));
            qrCode.setWidth(100);
            qrCode.setHeight(100);
            headerTable.addCell(qrCode);
        } catch (Exception e) {
            log.error("Error generating QR code: {}", e.getMessage());
            headerTable.addCell(new Paragraph("QR CODE"));
        }
        
        document.add(headerTable);
        document.add(new Paragraph("\n"));
    }

    /**
     * Generates QR code containing the verification URL
     */
    private byte[] generateQRCode(Long inscriptionId) throws WriterException, IOException {
        String url = qrCodeBaseUrl + inscriptionId;
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, 200, 200);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        return outputStream.toByteArray();
    }

    /**
     * Adds the title section
     */
    private void addTitle(Document document) {
        Paragraph title = new Paragraph("ATTESTATION D'INSCRIPTION")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(new DeviceRgb(0, 51, 102));
        
        document.add(title);
        document.add(new Paragraph("\n"));
        
        Paragraph subtitle = new Paragraph("Année Universitaire " + getCurrentAcademicYear())
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setItalic();
        
        document.add(subtitle);
        document.add(new Paragraph("\n\n"));
    }

    /**
     * Adds student information section
     */
    private void addStudentInformation(Document document, Inscription inscription, InfosDoctorant infosDoctorant) {
        Paragraph intro = new Paragraph("Le Directeur du Centre d'Études Doctorales atteste que :")
                .setFontSize(12);
        document.add(intro);
        document.add(new Paragraph("\n"));
        
        // Student details table
        Table studentTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}));
        studentTable.setWidth(UnitValue.createPercentValue(100));
        
        addTableRow(studentTable, "Nom et Prénom :", getStudentFullName(inscription));
        addTableRow(studentTable, "CIN :", infosDoctorant.getCin());
        
        if (infosDoctorant.getCne() != null && !infosDoctorant.getCne().isEmpty()) {
            addTableRow(studentTable, "CNE :", infosDoctorant.getCne());
        }
        
        addTableRow(studentTable, "Année d'étude :", String.valueOf(inscription.getAnneeInscription()));
        addTableRow(studentTable, "Type d'inscription :", inscription.getType().toString());
        
        document.add(studentTable);
        document.add(new Paragraph("\n"));
    }

    /**
     * Adds thesis details section
     */
    private void addThesisDetails(Document document, Inscription inscription) {
        Paragraph thesisHeader = new Paragraph("Informations sur la thèse :")
                .setFontSize(12)
                .setBold();
        document.add(thesisHeader);
        document.add(new Paragraph("\n"));
        
        Table thesisTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}));
        thesisTable.setWidth(UnitValue.createPercentValue(100));
        
        addTableRow(thesisTable, "Sujet de thèse :", inscription.getSujetThese());
        
        if (inscription.getInfosThese() != null) {
            addTableRow(thesisTable, "Discipline :", inscription.getInfosThese().getDiscipline());
            addTableRow(thesisTable, "Laboratoire :", inscription.getInfosThese().getLaboratoire());
            addTableRow(thesisTable, "Établissement :", inscription.getInfosThese().getEtablissementAccueil());
        }
        
        document.add(thesisTable);
        document.add(new Paragraph("\n"));
    }

    /**
     * Adds director information section
     */
    private void addDirectorInformation(Document document, UserDTO directeur) {
        Paragraph directorHeader = new Paragraph("Sous la direction de :")
                .setFontSize(12)
                .setBold();
        document.add(directorHeader);
        document.add(new Paragraph("\n"));
        
        String directorName = directeur.getFirstName() + " " + directeur.getLastName();
        Paragraph directorInfo = new Paragraph(directorName)
                .setFontSize(12);
        document.add(directorInfo);
        document.add(new Paragraph("\n\n"));
    }

    /**
     * Adds signature and stamp section
     */
    private void addSignatureSection(Document document, Inscription inscription) {
        // Validation date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String validationDate = inscription.getDateValidation() != null 
                ? inscription.getDateValidation().format(formatter)
                : LocalDateTime.now().format(formatter);
        
        Paragraph dateParagraph = new Paragraph("Fait à Casablanca, le " + validationDate)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.RIGHT);
        document.add(dateParagraph);
        document.add(new Paragraph("\n"));
        
        // Signature section
        Table signatureTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        signatureTable.setWidth(UnitValue.createPercentValue(100));
        
        // Left side - stamp placeholder
        Paragraph stampPlaceholder = new Paragraph("Cachet de l'établissement")
                .setFontSize(10)
                .setItalic()
                .setTextAlignment(TextAlignment.CENTER);
        signatureTable.addCell(stampPlaceholder);
        
        // Right side - signature
        try {
            Resource signatureResource = resourceLoader.getResource(signaturePath);
            if (signatureResource.exists()) {
                Image signature = new Image(ImageDataFactory.create(signatureResource.getURL()));
                signature.setWidth(150);
                signature.setHeight(75);
                signatureTable.addCell(signature);
            } else {
                log.warn("Signature not found at: {}", signaturePath);
                Paragraph signaturePlaceholder = new Paragraph("Le Directeur du CED\n\nSignature")
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER);
                signatureTable.addCell(signaturePlaceholder);
            }
        } catch (Exception e) {
            log.warn("Could not load signature: {}", e.getMessage());
            Paragraph signaturePlaceholder = new Paragraph("Le Directeur du CED\n\nSignature")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER);
            signatureTable.addCell(signaturePlaceholder);
        }
        
        document.add(signatureTable);
        
        // Footer note
        document.add(new Paragraph("\n\n"));
        Paragraph footer = new Paragraph("Cette attestation est valable pour l'année universitaire en cours.")
                .setFontSize(9)
                .setItalic()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY);
        document.add(footer);
    }

    /**
     * Helper method to add a row to a table
     */
    private void addTableRow(Table table, String label, String value) {
        table.addCell(new Paragraph(label).setBold().setFontSize(11));
        table.addCell(new Paragraph(value != null ? value : "N/A").setFontSize(11));
    }

    /**
     * Gets the current academic year (e.g., "2024-2025")
     */
    private String getCurrentAcademicYear() {
        int currentYear = LocalDateTime.now().getYear();
        int currentMonth = LocalDateTime.now().getMonthValue();
        
        // Academic year starts in September
        if (currentMonth >= 9) {
            return currentYear + "-" + (currentYear + 1);
        } else {
            return (currentYear - 1) + "-" + currentYear;
        }
    }

    /**
     * Gets the full name of the student from user service
     */
    private String getStudentFullName(Inscription inscription) {
        try {
            UserDTO student = userServiceClient.getStudentInfo(inscription.getDoctorantId());
            return student.getFirstName() + " " + student.getLastName();
        } catch (Exception e) {
            log.warn("Could not fetch student information for ID {}: {}", 
                    inscription.getDoctorantId(), e.getMessage());
            return "Étudiant " + inscription.getDoctorantId();
        }
    }
}
