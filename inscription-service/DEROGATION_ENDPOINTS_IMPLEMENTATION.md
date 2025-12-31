# Derogation REST Endpoints Implementation

## Overview
This document describes the implementation of the derogation REST endpoints for the inscription-service, as specified in task 8 of the inscription-service-finalisation spec.

## Implemented Components

### 1. DTOs Created

#### DerogationRequestDTO
- **Purpose**: Request body for creating a derogation
- **Location**: `src/main/java/ma/emsi/inscriptionservice/DTOs/DerogationRequestDTO.java`
- **Fields**:
  - `motif` (String, required, 50-2000 characters): Reason for requesting derogation
- **Validation**: Uses Jakarta validation annotations

#### DerogationValidationDTO
- **Purpose**: Request body for validating/rejecting a derogation
- **Location**: `src/main/java/ma/emsi/inscriptionservice/DTOs/DerogationValidationDTO.java`
- **Fields**:
  - `approuve` (Boolean, required): True to approve, false to reject
  - `commentaire` (String, optional, max 1000 characters): Validator's comment
- **Validation**: Uses Jakarta validation annotations

#### DerogationResponse
- **Purpose**: Response body for derogation operations
- **Location**: `src/main/java/ma/emsi/inscriptionservice/DTOs/DerogationResponse.java`
- **Fields**:
  - `id` (Long): Derogation ID
  - `inscriptionId` (Long): Associated inscription ID
  - `motif` (String): Reason for derogation
  - `statut` (StatutDerogation): Current status
  - `dateDemande` (LocalDateTime): Request date
  - `validateurId` (Long): Validator user ID
  - `commentaireValidation` (String): Validation comment
  - `dateValidation` (LocalDateTime): Validation date
  - `hasDocuments` (boolean): Whether supporting documents are attached

### 2. REST Endpoints Added to InscriptionController

#### POST /api/inscriptions/{id}/derogation
- **Purpose**: Create a new derogation request for an inscription
- **Authorization**: DOCTORANT role required
- **Path Parameter**: `id` - Inscription ID
- **Request Body**: `DerogationRequestDTO`
- **Optional Parameter**: `documents` (MultipartFile) - Supporting documents
- **Response**: `DerogationResponse` with HTTP 201 (Created)
- **Validates**: Requirements 3.1

#### POST /api/inscriptions/{id}/derogation/valider-directeur
- **Purpose**: Director validates or rejects a derogation request
- **Authorization**: DIRECTEUR role required
- **Path Parameter**: `id` - Inscription ID
- **Query Parameter**: `directeurId` - Director's user ID
- **Request Body**: `DerogationValidationDTO`
- **Response**: `DerogationResponse` with HTTP 200 (OK)
- **Validates**: Requirements 3.3, 3.4

#### POST /api/inscriptions/{id}/derogation/valider-ped
- **Purpose**: PED validates or rejects a derogation request
- **Authorization**: ADMIN role required (PED administrators)
- **Path Parameter**: `id` - Inscription ID
- **Request Body**: `DerogationValidationDTO`
- **Response**: `DerogationResponse` with HTTP 200 (OK)
- **Validates**: Requirements 3.5, 3.6

#### GET /api/inscriptions/{id}/derogation
- **Purpose**: Retrieve derogation request for an inscription
- **Authorization**: DOCTORANT, DIRECTEUR, or ADMIN role required
- **Path Parameter**: `id` - Inscription ID
- **Response**: `DerogationResponse` with HTTP 200 (OK), or HTTP 404 if not found
- **Validates**: Requirements 3.1

### 3. Helper Methods

#### mapToDerogationResponse()
- **Purpose**: Convert DerogationRequest entity to DerogationResponse DTO
- **Location**: Private method in InscriptionController
- **Logic**: Maps all fields and determines if documents are present

### 4. Authorization Checks

All endpoints implement proper authorization:
- **Create derogation**: Only students (DOCTORANT) can create
- **Validate by director**: Only directors (DIRECTEUR) can validate
- **Validate by PED**: Only administrators (ADMIN) can validate
- **Get derogation**: Students, directors, and admins can view

### 5. Integration Tests

#### DerogationEndpointsTest
- **Location**: `src/test/java/ma/emsi/inscriptionservice/controllers/DerogationEndpointsTest.java`
- **Test Cases**:
  1. `testCreerDerogation_Success`: Tests successful derogation creation
  2. `testValiderDerogationParDirecteur_Approve`: Tests director approval
  3. `testValiderDerogationParDirecteur_Reject`: Tests director rejection
  4. `testValiderDerogationParPED_Approve`: Tests PED approval
  5. `testGetDerogation_Found`: Tests retrieving existing derogation
  6. `testGetDerogation_NotFound`: Tests 404 response for non-existent derogation

## API Usage Examples

### Create Derogation
```bash
POST /api/inscriptions/1/derogation
Authorization: Bearer <doctorant-token>
Content-Type: application/json

{
  "motif": "Je demande une dérogation car j'ai eu des problèmes de santé qui ont retardé mes recherches pendant 6 mois."
}
```

### Director Validates Derogation
```bash
POST /api/inscriptions/1/derogation/valider-directeur?directeurId=5
Authorization: Bearer <directeur-token>
Content-Type: application/json

{
  "approuve": true,
  "commentaire": "J'approuve cette demande compte tenu des circonstances."
}
```

### PED Validates Derogation
```bash
POST /api/inscriptions/1/derogation/valider-ped
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "approuve": true,
  "commentaire": "Le PED approuve cette dérogation."
}
```

### Get Derogation
```bash
GET /api/inscriptions/1/derogation
Authorization: Bearer <token>
```

## Error Handling

All endpoints use the existing exception handler in InscriptionController:
- Invalid requests return HTTP 400 with error message
- Not found resources return HTTP 404
- Unauthorized access is handled by Spring Security

## Integration with Existing Services

The endpoints integrate with:
- **DerogationService**: All business logic for derogation workflow
- **Spring Security**: Role-based authorization
- **Kafka**: Notifications sent via DerogationService
- **UserServiceClient**: Fetching user information for notifications

## Requirements Coverage

This implementation satisfies the following requirements from the design document:
- **Requirement 3.1**: Create derogation request
- **Requirement 3.3**: Director approval workflow
- **Requirement 3.4**: Director rejection workflow
- **Requirement 3.5**: PED approval workflow
- **Requirement 3.6**: PED rejection workflow

## Notes

1. The `documents` parameter in the create endpoint is optional and handled as MultipartFile
2. All validation is performed by the DerogationService layer
3. Notifications are automatically sent via Kafka by the service layer
4. The endpoints follow the same patterns as existing inscription endpoints
5. Authorization is enforced at the controller level using Spring Security annotations
