# Alert Verification Endpoint Documentation

## Overview

This document describes the batch alert verification endpoint that was implemented as part of task 13 of the inscription-service-finalisation specification.

## Endpoint Details

### URL
```
GET /api/inscriptions/verifier-alertes
```

### Authorization
- **Required Role**: `ADMIN`
- Only administrators can access this endpoint

### Description
This endpoint performs batch processing to verify and generate duration alerts for all active inscriptions in the system. It checks all inscriptions with the following statuses:
- `EN_ATTENTE_DIRECTEUR` (Waiting for director validation)
- `EN_ATTENTE_ADMIN` (Waiting for admin validation)
- `VALIDE` (Validated)

### Requirements Addressed
- **Requirement 4.1**: WHEN a student reaches 2.5 years of study THEN the system SHALL create an alert of type APPROCHE_3_ANS
- **Requirement 4.2**: WHEN a student reaches 5.5 years of study THEN the system SHALL create an alert of type APPROCHE_6_ANS
- **Requirement 4.3**: WHEN a student reaches 6 years of study THEN the system SHALL create an alert of type DEPASSE_6_ANS and block re-registration

## Response Format

### Success Response (200 OK)

```json
{
  "totalInscriptionsVerifiees": 150,
  "totalAlertesGenerees": 23,
  "alertesParType": {
    "APPROCHE_3_ANS": 15,
    "APPROCHE_6_ANS": 7,
    "DEPASSE_6_ANS": 1
  },
  "inscriptionsBloqueees": 1,
  "dateVerification": "2024-12-31T10:30:00",
  "dureeTraitementMs": 2345,
  "message": "Vérification terminée: 150 inscriptions vérifiées, 23 alertes générées, 1 inscriptions bloquées"
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `totalInscriptionsVerifiees` | Integer | Total number of inscriptions that were checked |
| `totalAlertesGenerees` | Integer | Total number of new alerts generated during this verification |
| `alertesParType` | Map<String, Integer> | Breakdown of alerts by type (APPROCHE_3_ANS, APPROCHE_6_ANS, DEPASSE_6_ANS) |
| `inscriptionsBloqueees` | Integer | Number of inscriptions that were blocked due to exceeding 6 years |
| `dateVerification` | LocalDateTime | Timestamp when the verification was performed |
| `dureeTraitementMs` | Long | Duration of the batch processing in milliseconds |
| `message` | String | Human-readable summary message |

### Error Response (500 Internal Server Error)

```json
{
  "totalInscriptionsVerifiees": 0,
  "totalAlertesGenerees": 0,
  "alertesParType": {},
  "inscriptionsBloqueees": 0,
  "dateVerification": "2024-12-31T10:30:00",
  "dureeTraitementMs": 0,
  "message": "Erreur lors de la vérification: Database connection failed"
}
```

## Usage Examples

### Using cURL

```bash
curl -X GET "http://localhost:8082/api/inscriptions/verifier-alertes" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

### Using Postman

1. Set method to `GET`
2. Enter URL: `http://localhost:8082/api/inscriptions/verifier-alertes`
3. Add Authorization header with JWT token (role: ADMIN)
4. Send request

### Using Java/Spring RestTemplate

```java
RestTemplate restTemplate = new RestTemplate();
HttpHeaders headers = new HttpHeaders();
headers.setBearerAuth(jwtToken);
HttpEntity<String> entity = new HttpEntity<>(headers);

ResponseEntity<AlerteVerificationSummary> response = restTemplate.exchange(
    "http://localhost:8082/api/inscriptions/verifier-alertes",
    HttpMethod.GET,
    entity,
    AlerteVerificationSummary.class
);

AlerteVerificationSummary summary = response.getBody();
System.out.println("Inscriptions vérifiées: " + summary.getTotalInscriptionsVerifiees());
```

## Implementation Details

### Service Layer

The endpoint delegates to two main service methods:

1. **InscriptionService.getInscriptionsByStatuts(List<StatutInscription>)**
   - Retrieves all inscriptions matching the specified statuses
   - Returns a list of active inscriptions

2. **InscriptionService.verifierAlertesEnBatch(List<Inscription>)**
   - Delegates to AlerteService.verifierAlertesEnBatch()
   - Processes each inscription to check duration thresholds
   - Generates alerts as needed
   - Returns a summary of the verification

### Alert Generation Logic

For each inscription, the system:
1. Calculates the duration since first inscription (in years with decimals)
2. Checks against three thresholds:
   - **2.5 years**: Creates APPROCHE_3_ANS alert if not already exists
   - **5.5 years**: Creates APPROCHE_6_ANS alert if not already exists
   - **6.0 years**: Creates DEPASSE_6_ANS alert and blocks re-registration
3. Publishes Kafka notification events for each alert
4. Prevents duplicate alerts using idempotency checks

### Transaction Management

- The batch verification is wrapped in a `@Transactional` annotation
- If an error occurs for one inscription, it's logged and processing continues
- The transaction ensures data consistency

## Scheduled Execution

This endpoint is designed to be called:
- **Manually** by administrators through the API
- **Automatically** by a scheduled batch service (e.g., daily at 8:00 AM)
- **On-demand** for testing or troubleshooting

## Performance Considerations

- Processing time scales linearly with the number of active inscriptions
- Average processing time: ~15-20ms per inscription
- For 1000 inscriptions: ~15-20 seconds
- Database queries are optimized with proper indexing
- Kafka notifications are sent asynchronously

## Security

- Endpoint is protected by Spring Security
- Only users with `ADMIN` role can access
- JWT token must be valid and not expired
- Authorization is checked before any processing begins

## Testing

Unit tests are provided in:
```
inscription-service/src/test/java/ma/emsi/inscriptionservice/controllers/AlerteVerificationEndpointTest.java
```

Test coverage includes:
- Authorization checks (401, 403 responses)
- Successful batch processing (200 response)
- Error handling (500 response)
- Empty result sets
- Different user roles

## Related Components

- **AlerteService**: Core alert generation logic
- **InscriptionService**: Inscription retrieval and coordination
- **AlerteDureeRepository**: Database access for alerts
- **InscriptionRepository**: Database access for inscriptions
- **NotificationService**: Kafka event publishing

## Future Enhancements

Potential improvements for future versions:
1. Add pagination for very large datasets
2. Add filtering by date range or specific doctorants
3. Add dry-run mode to preview alerts without creating them
4. Add detailed logging/audit trail
5. Add metrics/monitoring integration
