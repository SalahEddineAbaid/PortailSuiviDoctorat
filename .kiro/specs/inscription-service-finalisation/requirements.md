# Requirements Document

## Introduction

This specification defines the requirements for finalizing the inscription-service, which manages doctoral student registrations, re-registrations, and their validation workflow. The service is currently at 85% completion and requires critical enhancements including automatic document validation, attestation generation, derogation workflow, duration alerts, and enhanced dashboard capabilities.

## Glossary

- **Inscription-Service**: The microservice responsible for managing doctoral student registrations and re-registrations
- **Doctorant**: A doctoral student enrolled in the PhD program
- **Directeur**: The thesis director/supervisor responsible for validating student registrations
- **PED**: Pôle Études Doctorales (Doctoral Studies Department) - administrative entity responsible for final validation
- **Campagne**: A registration campaign with defined start and end dates
- **Dérogation**: An exception request allowing a student to continue beyond the standard 3-year period
- **Attestation**: An official certificate document proving student registration status
- **MIME Type**: Multipurpose Internet Mail Extensions type - a standard for identifying file formats
- **QR Code**: Quick Response code - a two-dimensional barcode for encoding information
- **Kafka**: A distributed event streaming platform used for inter-service communication
- **Feign Client**: A declarative HTTP client for making REST API calls between microservices

## Requirements

### Requirement 1

**User Story:** As a system administrator, I want automatic validation of uploaded documents, so that only valid and safe files are accepted into the system.

#### Acceptance Criteria

1. WHEN a user uploads a document THEN the Inscription-Service SHALL verify the MIME type matches one of the allowed types (application/pdf, image/jpeg, image/png)
2. WHEN a user uploads a document THEN the Inscription-Service SHALL verify the file size does not exceed 10 MB
3. WHEN a document fails MIME type validation THEN the Inscription-Service SHALL reject the upload and return the error message "Le fichier doit être au format PDF ou image (JPEG/PNG)"
4. WHEN a document fails size validation THEN the Inscription-Service SHALL reject the upload and return the error message "La taille du fichier ne doit pas dépasser 10 MB"
5. WHEN a document passes all validations THEN the Inscription-Service SHALL rename the file using the pattern {type}_{timestamp}_{userId}_{random}.{extension}

### Requirement 2

**User Story:** As a doctoral student, I want to automatically receive an official attestation PDF when my registration is validated, so that I have proof of my enrollment status.

#### Acceptance Criteria

1. WHEN an inscription status changes to VALIDE THEN the Inscription-Service SHALL generate an attestation PDF document
2. WHEN generating an attestation THEN the Inscription-Service SHALL include the student name, CIN, CNE, year of study, academic year, thesis subject, and director information
3. WHEN generating an attestation THEN the Inscription-Service SHALL embed a QR code containing the verification URL https://portail.emsi.ma/verify/attestation/{id}
4. WHEN an attestation is generated THEN the Inscription-Service SHALL store the PDF file at path ./uploads/attestations/{inscriptionId}_{timestamp}.pdf
5. WHEN an attestation is generated THEN the Inscription-Service SHALL record the file path in the documents_generes table
6. WHEN a user requests an attestation download THEN the Inscription-Service SHALL verify the user is the student, their director, or an administrator
7. WHEN an authorized user requests an attestation THEN the Inscription-Service SHALL return the PDF file with Content-Type application/pdf

### Requirement 3

**User Story:** As a doctoral student exceeding 3 years, I want to submit a formal derogation request, so that I can continue my studies with proper approval.

#### Acceptance Criteria

1. WHEN a student submits a derogation request THEN the Inscription-Service SHALL create a DerogationRequest entity with status EN_ATTENTE
2. WHEN a derogation request is created THEN the Inscription-Service SHALL send a notification to the thesis director via Kafka
3. WHEN a director validates a derogation request THEN the Inscription-Service SHALL update the status to APPROUVE_DIRECTEUR and send a notification to PED administrators
4. WHEN a director rejects a derogation request THEN the Inscription-Service SHALL update the status to REJETE and send a notification to the student
5. WHEN a PED administrator validates a derogation request THEN the Inscription-Service SHALL update the status to APPROUVE_PED and set the inscription derogation flag to true
6. WHEN a PED administrator rejects a derogation request THEN the Inscription-Service SHALL update the status to REJETE and block re-registration
7. WHEN a student attempts re-registration with duration exceeding 3 years without approved derogation THEN the Inscription-Service SHALL reject the registration request

### Requirement 4

**User Story:** As a system administrator, I want automatic alerts for students approaching duration limits, so that students are proactively informed before reaching critical deadlines.

#### Acceptance Criteria

1. WHEN a student reaches 2.5 years of study THEN the Inscription-Service SHALL create an alert of type APPROCHE_3_ANS
2. WHEN a student reaches 5.5 years of study THEN the Inscription-Service SHALL create an alert of type APPROCHE_6_ANS
3. WHEN a student reaches 6 years of study THEN the Inscription-Service SHALL create an alert of type DEPASSE_6_ANS and block re-registration
4. WHEN an alert is created THEN the Inscription-Service SHALL publish a notification event to Kafka with alert details
5. WHEN verifying alerts for an inscription THEN the Inscription-Service SHALL not create duplicate alerts of the same type
6. WHEN a re-registration is submitted THEN the Inscription-Service SHALL verify and generate alerts for the student

### Requirement 5

**User Story:** As a doctoral student, I want a comprehensive dashboard showing my complete doctoral journey, so that I can track my progress and upcoming requirements.

#### Acceptance Criteria

1. WHEN a student requests their dashboard THEN the Inscription-Service SHALL return current inscription details including year, type, status, duration, and derogation status
2. WHEN a student requests their dashboard THEN the Inscription-Service SHALL return the complete history of all inscriptions ordered by year
3. WHEN a student requests their dashboard THEN the Inscription-Service SHALL return all active alerts with type, date, and message
4. WHEN a student requests their dashboard THEN the Inscription-Service SHALL return a list of missing required documents
5. WHEN a student requests their dashboard THEN the Inscription-Service SHALL calculate and return statistics including completion rate, validated documents count, and total documents count
6. WHEN a student requests their dashboard THEN the Inscription-Service SHALL return the next milestone with type, deadline date, and status

### Requirement 6

**User Story:** As a system administrator, I want enhanced campaign management capabilities, so that I can efficiently manage annual registration campaigns.

#### Acceptance Criteria

1. WHEN a campaign start date is reached THEN the Inscription-Service SHALL automatically send opening notifications to all eligible students
2. WHEN a campaign end date is reached THEN the Inscription-Service SHALL automatically send closing notifications and set the campaign active flag to false
3. WHEN an administrator requests campaign statistics THEN the Inscription-Service SHALL return the total number of inscriptions, breakdown by status, validation rate, and average validation time
4. WHEN an administrator clones a campaign THEN the Inscription-Service SHALL create a new campaign copying the type and libelle with incremented year
5. WHEN the scheduled task runs daily at 8:00 AM THEN the Inscription-Service SHALL verify all campaigns for opening or closing dates matching the current date

### Requirement 7

**User Story:** As a system integrator, I want proper Kafka event publishing for all inscription lifecycle events, so that other services can react to registration changes.

#### Acceptance Criteria

1. WHEN an inscription is submitted THEN the Inscription-Service SHALL publish an INSCRIPTION_SOUMISE event containing inscriptionId, doctorantId, and directeurId
2. WHEN a director validates an inscription THEN the Inscription-Service SHALL publish an INSCRIPTION_VALIDEE_DIRECTEUR event
3. WHEN a director rejects an inscription THEN the Inscription-Service SHALL publish an INSCRIPTION_REJETEE_DIRECTEUR event with rejection reason
4. WHEN an administrator validates an inscription THEN the Inscription-Service SHALL publish an INSCRIPTION_VALIDEE_ADMIN event
5. WHEN an administrator rejects an inscription THEN the Inscription-Service SHALL publish an INSCRIPTION_REJETEE_ADMIN event with rejection reason
6. WHEN a campaign opens THEN the Inscription-Service SHALL publish a CAMPAGNE_OUVERTE event
7. WHEN a campaign closes THEN the Inscription-Service SHALL publish a CAMPAGNE_FERMEE event
8. WHEN a derogation is requested THEN the Inscription-Service SHALL publish a DEROGATION_DEMANDEE event
9. WHEN a duration alert is created THEN the Inscription-Service SHALL publish an ALERTE_DUREE event

### Requirement 8

**User Story:** As a system administrator, I want configurable validation rules and PDF generation settings, so that the system can be adapted to different institutional requirements.

#### Acceptance Criteria

1. WHEN the application starts THEN the Inscription-Service SHALL load allowed document types from the upload.allowed-types configuration property
2. WHEN the application starts THEN the Inscription-Service SHALL load maximum file size from the upload.max-size configuration property
3. WHEN the application starts THEN the Inscription-Service SHALL load PDF generation settings including logo path, signature path, and QR code base URL
4. WHEN the application starts THEN the Inscription-Service SHALL load alert duration thresholds from configuration properties
5. WHERE virus scanning is enabled THEN the Inscription-Service SHALL connect to ClamAV using the configured host and port
