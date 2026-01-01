# Requirements Document

## Introduction

This document specifies the requirements for finalizing the defense-service microservice in the doctoral thesis management system. The defense-service currently handles defense requests, prerequisites, jury composition, evaluation reports, and defense scheduling at 75% completion. This specification covers the remaining 25% including publication quality verification, complete authorization workflow, process-verbal generation, director dashboard, and statistics reporting.

## Glossary

- **Defense_Service**: The microservice responsible for managing thesis defense workflows
- **Doctorant**: A doctoral student pursuing a PhD degree
- **Directeur**: The thesis director/supervisor responsible for guiding the doctoral student
- **Admin**: The administrative staff member who validates and authorizes defenses
- **Jury**: The committee of professors who evaluate the thesis defense
- **Rapporteur**: A jury member responsible for writing an evaluation report
- **Prerequisites**: Required academic achievements (publications, conferences, training) before defense
- **Q1/Q2**: Top-tier journal quality quartiles (Q1 = top 25%, Q2 = 25-50%)
- **Defense_Request**: A formal request submitted by a doctoral student to schedule their defense
- **Autorisation_Soutenance**: The official authorization document allowing a defense to proceed
- **Proces_Verbal**: The official minutes document generated after a defense is completed
- **User_Service**: The microservice managing user authentication and profiles
- **Notification_Service**: The microservice handling notifications via Kafka events

## Requirements

### Requirement 1: Publication Quality Verification

**User Story:** As an administrator, I want to manually validate the quality of doctoral publications, so that I can ensure compliance with Q1/Q2 journal requirements before authorizing defenses.

#### Acceptance Criteria

1. WHEN a doctorant adds a publication to prerequisites, THE Defense_Service SHALL store the publication with title, journal, year, type, quartile, DOI, and URL
2. WHEN a publication is created, THE Defense_Service SHALL set its validation status to false by default
3. WHEN an admin validates a publication, THE Defense_Service SHALL record the validator ID, verification date, verified quartile, and validation comments
4. WHEN counting valid Q1/Q2 publications, THE Defense_Service SHALL only include publications where validation status is true and quartile is Q1 or Q2
5. WHEN an admin requests pending validations, THE Defense_Service SHALL return all publications where validation status is false
6. WHEN checking prerequisites validity, THE Defense_Service SHALL verify at least 2 validated journal articles with Q1 or Q2 quartile exist
7. WHEN a publication type is journal article, THE Defense_Service SHALL require a quartile value from the set {Q1, Q2, Q3, Q4, NON_CLASSE}
8. WHEN an admin validates a publication, THE Defense_Service SHALL allow updating the quartile if verification reveals a different classification

### Requirement 2: Complete Authorization Workflow

**User Story:** As an administrator, I want to verify all defense prerequisites and issue official authorization, so that defenses can proceed only when all conditions are met.

#### Acceptance Criteria

1. WHEN an admin requests authorization verification, THE Defense_Service SHALL check that prerequisites are validated, jury is complete, reports are favorable, and documents are uploaded
2. WHEN verifying jury completeness, THE Defense_Service SHALL ensure at least one president, two rapporteurs, and one examiner are assigned
3. WHEN verifying reports, THE Defense_Service SHALL ensure all rapporteurs have submitted reports with favorable recommendations
4. WHEN an admin authorizes a defense, THE Defense_Service SHALL create an Autorisation_Soutenance entity with status AUTORISE
5. WHEN an admin authorizes a defense, THE Defense_Service SHALL update the Defense_Request status to AUTHORIZED
6. WHEN an admin authorizes a defense, THE Defense_Service SHALL create a Defense entity with the scheduled date, location, and room
7. WHEN an admin authorizes a defense, THE Defense_Service SHALL publish a SOUTENANCE_AUTORISEE event to Kafka with doctorant, directeur, and jury member IDs
8. WHEN an admin refuses authorization, THE Defense_Service SHALL create an Autorisation_Soutenance entity with status REFUSE and record the refusal reason
9. WHEN an admin refuses authorization, THE Defense_Service SHALL update the Defense_Request status to REJECTED
10. WHEN an admin refuses authorization, THE Defense_Service SHALL publish a SOUTENANCE_REFUSEE event to Kafka with the refusal reason
11. WHEN authorization verification fails any check, THE Defense_Service SHALL return detailed information about which checks failed and why
12. WHEN an authorization is created, THE Defense_Service SHALL record the administrator ID and authorization timestamp

### Requirement 3: Process-Verbal Generation

**User Story:** As an administrator, I want to automatically generate the official defense minutes (procès-verbal) after a defense is completed, so that I can provide official documentation of the defense outcome.

#### Acceptance Criteria

1. WHEN an admin finalizes a defense, THE Defense_Service SHALL record the mention, publication recommendation, jury comments, and deliberation date
2. WHEN an admin finalizes a defense, THE Defense_Service SHALL generate a PDF procès-verbal document
3. WHEN generating a procès-verbal, THE Defense_Service SHALL include doctorant information (name, CIN), thesis title, defense date and location, complete jury composition with roles, defense outcome, mention awarded, and jury recommendations
4. WHEN generating a procès-verbal, THE Defense_Service SHALL include space for jury member signatures
5. WHEN a defense is finalized, THE Defense_Service SHALL update the Defense status to COMPLETED
6. WHEN a defense is finalized, THE Defense_Service SHALL publish a SOUTENANCE_FINALISEE event to Kafka with doctorant ID and mention
7. WHEN a user requests a procès-verbal download, THE Defense_Service SHALL verify the user is a jury member, the doctorant, the directeur, or an admin
8. WHEN a procès-verbal is requested for a non-finalized defense, THE Defense_Service SHALL return an error indicating the defense is not yet completed

### Requirement 4: Director Dashboard

**User Story:** As a thesis director, I want to view a consolidated dashboard of all my doctoral students' defense progress, so that I can track their status and identify actions I need to take.

#### Acceptance Criteria

1. WHEN a directeur requests their dashboard, THE Defense_Service SHALL return statistics including active doctorants count, defenses to plan, pending reports, and juries to propose
2. WHEN a directeur requests their dashboard, THE Defense_Service SHALL return all defense requests for their doctorants with current status and next action
3. WHEN a directeur requests their dashboard, THE Defense_Service SHALL return all scheduled defenses for their doctorants with date and location
4. WHEN a directeur requests their dashboard, THE Defense_Service SHALL return alerts for overdue reports or pending actions
5. WHEN displaying defense requests, THE Defense_Service SHALL include doctorant name, request status, doctorate start date, duration, and next required action
6. WHEN calculating alerts, THE Defense_Service SHALL identify rapporteurs who have not submitted reports within expected timeframes

### Requirement 5: Statistics and Reporting

**User Story:** As an administrator, I want to view comprehensive statistics and generate reports about defenses, so that I can analyze trends and provide institutional reporting.

#### Acceptance Criteria

1. WHEN an admin requests statistics, THE Defense_Service SHALL return defense request counts grouped by status
2. WHEN an admin requests statistics, THE Defense_Service SHALL return defense counts grouped by month for the current year
3. WHEN an admin requests statistics, THE Defense_Service SHALL return mention distribution counts across all completed defenses
4. WHEN an admin requests statistics, THE Defense_Service SHALL calculate the overall success rate as a percentage
5. WHEN an admin requests statistics, THE Defense_Service SHALL calculate the average duration from doctorate start to defense completion
6. WHEN an admin requests a monthly report, THE Defense_Service SHALL generate a PDF report including defense list, statistics, and visualizations for the specified month
7. WHEN an admin requests an annual report, THE Defense_Service SHALL generate a PDF report including yearly evolution, comparisons, and comprehensive analysis

### Requirement 6: Kafka Event Publishing

**User Story:** As the defense-service, I want to publish events to Kafka for all significant defense workflow changes, so that the notification-service can inform relevant stakeholders.

#### Acceptance Criteria

1. WHEN a defense request is submitted, THE Defense_Service SHALL publish a DEMANDE_SOUTENANCE_SOUMISE event with defense request ID, doctorant ID, and directeur ID
2. WHEN a jury is proposed, THE Defense_Service SHALL publish a JURY_PROPOSE event with defense request ID and all jury member IDs
3. WHEN a rapport is submitted, THE Defense_Service SHALL publish a RAPPORT_SOUMIS event with rapport ID, defense request ID, rapporteur ID, and favorable status
4. WHEN a defense is authorized, THE Defense_Service SHALL publish a SOUTENANCE_AUTORISEE event with defense request ID, doctorant ID, directeur ID, jury member IDs, and defense date
5. WHEN a defense authorization is refused, THE Defense_Service SHALL publish a SOUTENANCE_REFUSEE event with defense request ID, doctorant ID, and refusal reason
6. WHEN a defense is finalized, THE Defense_Service SHALL publish a SOUTENANCE_FINALISEE event with defense ID, doctorant ID, and mention awarded
7. WHEN publishing events to Kafka, THE Defense_Service SHALL use the topic name "defense-events"

### Requirement 7: Resilience and Fault Tolerance

**User Story:** As the defense-service, I want to maintain resilience when communicating with user-service, so that temporary failures do not disrupt defense workflows.

#### Acceptance Criteria

1. WHEN the User_Service is unavailable, THE Defense_Service SHALL use circuit breaker pattern to prevent cascading failures
2. WHEN the User_Service call fails, THE Defense_Service SHALL retry the request according to configured retry policy
3. WHEN the User_Service is unavailable after retries, THE Defense_Service SHALL execute fallback logic to return cached or default user information
4. WHEN the circuit breaker is open, THE Defense_Service SHALL return fallback responses without attempting User_Service calls
5. WHEN enriching defense data with user information, THE Defense_Service SHALL gracefully handle User_Service failures without blocking defense operations

### Requirement 8: Configuration Management

**User Story:** As a system administrator, I want to configure defense prerequisites and jury composition rules externally, so that I can adjust requirements without code changes.

#### Acceptance Criteria

1. THE Defense_Service SHALL read the minimum required Q1/Q2 publications from configuration property "defense.prerequis.min-publications-q1q2"
2. THE Defense_Service SHALL read the minimum required conferences from configuration property "defense.prerequis.min-conferences"
3. THE Defense_Service SHALL read the minimum required training hours from configuration property "defense.prerequis.min-heures-formation"
4. THE Defense_Service SHALL read the minimum jury members from configuration property "defense.jury.min-membres"
5. THE Defense_Service SHALL read the minimum rapporteurs from configuration property "defense.jury.min-rapporteurs"
6. WHEN validating prerequisites or jury composition, THE Defense_Service SHALL use the configured values rather than hardcoded constants
