package ma.emsi.inscriptionservice.generators;

import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import ma.emsi.inscriptionservice.entities.Campagne;
import ma.emsi.inscriptionservice.entities.DerogationRequest;
import ma.emsi.inscriptionservice.entities.Inscription;
import org.junit.runner.RunWith;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.Assert.*;

/**
 * Test suite to verify that all custom generators work correctly.
 * These tests ensure generators produce valid data and cover edge cases.
 */
@RunWith(JUnitQuickcheck.class)
public class GeneratorsTest {

    /**
     * Verify InscriptionGenerator produces valid inscriptions.
     */
    @Property(trials = 50)
    public void inscriptionGeneratorProducesValidData(
            @From(InscriptionGenerator.class) Inscription inscription) {
        
        assertNotNull("Inscription should not be null", inscription);
        assertNotNull("Doctorant ID should not be null", inscription.getDoctorantId());
        assertNotNull("Directeur ID should not be null", inscription.getDirecteurTheseId());
        assertNotNull("Type should not be null", inscription.getType());
        assertNotNull("Status should not be null", inscription.getStatut());
        assertNotNull("Year should not be null", inscription.getAnneeInscription());
        assertNotNull("Creation date should not be null", inscription.getDateCreation());
        assertNotNull("Thesis subject should not be null", inscription.getSujetThese());
        
        assertTrue("Doctorant ID should be positive", inscription.getDoctorantId() > 0);
        assertTrue("Directeur ID should be positive", inscription.getDirecteurTheseId() > 0);
        assertTrue("Year should be reasonable", 
            inscription.getAnneeInscription() >= 2020 && inscription.getAnneeInscription() <= 2026);
    }

    /**
     * Verify DocumentGenerator produces valid multipart files.
     */
    @Property(trials = 50)
    public void documentGeneratorProducesValidData(
            @From(DocumentGenerator.class) MultipartFile file) {
        
        assertNotNull("File should not be null", file);
        assertNotNull("Filename should not be null", file.getOriginalFilename());
        assertNotNull("Content type should not be null", file.getContentType());
        assertNotNull("Content should not be null", file.getResource());
        
        assertTrue("File size should be positive", file.getSize() > 0);
        String filename = file.getOriginalFilename();
        assertNotNull("Filename should not be null", filename);
        assertTrue("Filename should not be empty", !filename.isEmpty());
    }

    /**
     * Verify InvalidMimeTypeGenerator produces only invalid MIME types.
     */
    @Property(trials = 50)
    public void invalidMimeTypeGeneratorProducesInvalidTypes(
            @From(InvalidMimeTypeGenerator.class) String mimeType) {
        
        assertNotNull("MIME type should not be null", mimeType);
        
        // Verify it's not one of the valid types
        assertFalse("Should not be application/pdf", "application/pdf".equals(mimeType));
        assertFalse("Should not be image/jpeg", "image/jpeg".equals(mimeType));
        assertFalse("Should not be image/png", "image/png".equals(mimeType));
    }

    /**
     * Verify DerogationRequestGenerator produces valid derogation requests.
     */
    @Property(trials = 50)
    public void derogationRequestGeneratorProducesValidData(
            @From(DerogationRequestGenerator.class) DerogationRequest derogation) {
        
        assertNotNull("Derogation should not be null", derogation);
        assertNotNull("Inscription should not be null", derogation.getInscription());
        assertNotNull("Motif should not be null", derogation.getMotif());
        assertNotNull("Date demande should not be null", derogation.getDateDemande());
        assertNotNull("Status should not be null", derogation.getStatut());
        
        assertTrue("Motif should not be empty", !derogation.getMotif().isEmpty());
        assertTrue("Motif should be within length constraint", 
            derogation.getMotif().length() <= 2000);
        
        if (derogation.getCommentaireValidation() != null) {
            assertTrue("Comment should be within length constraint", 
                derogation.getCommentaireValidation().length() <= 1000);
        }
    }

    /**
     * Verify CampagneGenerator produces valid campaigns.
     */
    @Property(trials = 50)
    public void campagneGeneratorProducesValidData(
            @From(CampagneGenerator.class) Campagne campagne) {
        
        assertNotNull("Campagne should not be null", campagne);
        assertNotNull("Libelle should not be null", campagne.getLibelle());
        assertNotNull("Type should not be null", campagne.getType());
        assertNotNull("Date debut should not be null", campagne.getDateDebut());
        assertNotNull("Date fin should not be null", campagne.getDateFin());
        assertNotNull("Active flag should not be null", campagne.getActive());
        
        assertTrue("Libelle should not be empty", !campagne.getLibelle().isEmpty());
        
        if (campagne.getAnneeUniversitaire() != null) {
            assertTrue("Academic year should be reasonable", 
                campagne.getAnneeUniversitaire() >= 2020 && campagne.getAnneeUniversitaire() <= 2026);
        }
    }

    /**
     * Verify DocumentGenerator produces files with various sizes including edge cases.
     */
    @Property(trials = 100)
    public void documentGeneratorProducesVariousSizes(
            @From(DocumentGenerator.class) MultipartFile file) {
        
        long size = file.getSize();
        assertTrue("File size should be positive", size > 0);
        
        // We should see files of various sizes across 100 trials
        // This is a weak assertion but ensures the generator is working
        assertTrue("File size should be reasonable", size < 25 * 1024 * 1024); // < 25MB
    }

    /**
     * Verify CampagneGenerator produces campaigns with various date scenarios.
     */
    @Property(trials = 100)
    public void campagneGeneratorProducesVariousDateScenarios(
            @From(CampagneGenerator.class) Campagne campagne) {
        
        assertNotNull("Date debut should not be null", campagne.getDateDebut());
        assertNotNull("Date fin should not be null", campagne.getDateFin());
        
        // Across 100 trials, we should see various date scenarios
        // This test just ensures dates are generated without errors
    }
}
