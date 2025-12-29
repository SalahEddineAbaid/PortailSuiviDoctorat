import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { SoutenanceService } from './soutenance.service';
import { environment } from '../../environments/environment';
import {
  SoutenanceRequest,
  SoutenanceResponse,
  DefenseScheduleDTO,
  DefenseResponseDTO,
  PrerequisStatus,
  JuryMemberRequest,
  SoutenanceStatus,
  JuryMember,
  JuryRole
} from '../models/soutenance.model';

describe('SoutenanceService', () => {
  let service: SoutenanceService;
  let httpMock: HttpTestingController;
  const API_URL = `${environment.apiUrl}/defense-service/defenses`;
  const SOUTENANCE_API_URL = `${environment.apiUrl}/soutenances`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        SoutenanceService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(SoutenanceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Soutenance Endpoints', () => {
    it('should create demande soutenance', () => {
      const mockRequest: SoutenanceRequest = {
        titrethese: 'Test Thesis Title',
        jury: []
      };

      const mockResponse: SoutenanceResponse = {
        id: 1,
        titrethese: 'Test Thesis Title',
        statut: SoutenanceStatus.BROUILLON,
        doctorant: {
          id: 1,
          FirstName: 'John',
          LastName: 'Doe',
          email: 'john@test.com',
          phoneNumber: '0123456789',
          adresse: 'Test Address',
          ville: 'Test City',
          pays: 'France',
          enabled: true,
          roles: []
        },
        directeur: {
          id: 2,
          FirstName: 'Jane',
          LastName: 'Smith',
          email: 'jane@test.com',
          phoneNumber: '0123456789',
          adresse: 'Test Address',
          ville: 'Test City',
          pays: 'France',
          enabled: true,
          roles: []
        },
        jury: [],
        
        prerequis: {
          prerequisRemplis: false,
          publicationsValides: false,
          heuresFormationValides: false,
          dureeDoctoratValide: true,
          documentsCompletsValides: false,
          details: []
        }
      };

      service.createDemandeSoutenance(mockRequest).subscribe(response => {
        expect(response).toEqual(mockResponse);
        expect(response.statut).toBe(SoutenanceStatus.BROUILLON);
      });

      const req = httpMock.expectOne(SOUTENANCE_API_URL);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(mockRequest);
      req.flush(mockResponse);
    });

    it('should schedule defense', () => {
      const mockRequest: DefenseScheduleDTO = {
        doctorantId: 1,
        directeurId: 2,
        titrethese: 'Test Thesis',
        dateSoutenance: new Date(),
        lieuSoutenance: 'Test Location',
        jury: []
      };

      const mockResponse: DefenseResponseDTO = {
        id: 1,
        doctorantId: 1,
        directeurId: 2,
        titrethese: 'Test Thesis',
        dateSoutenance: new Date(),
        lieuSoutenance: 'Test Location',
        statut: SoutenanceStatus.AUTORISEE,
        dateCreation: new Date()
      };

      service.scheduleDefense(mockRequest).subscribe(response => {
        expect(response).toEqual(mockResponse);
        expect(response.statut).toBe(SoutenanceStatus.AUTORISEE);
      });

      const req = httpMock.expectOne(API_URL);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(mockRequest);
      req.flush(mockResponse);
    });

    it('should get defense by request id', () => {
      const requestId = 1;
      const mockResponse: DefenseResponseDTO = {
        id: 1,
        doctorantId: requestId,
        directeurId: 2,
        titrethese: 'Test Thesis',
        dateSoutenance: new Date(),
        lieuSoutenance: 'Test Location',
        statut: SoutenanceStatus.AUTORISEE,
        dateCreation: new Date()
      };

      service.getDefenseByRequestId(requestId).subscribe(response => {
        expect(response).toEqual(mockResponse);
        expect(response.doctorantId).toBe(requestId);
      });

      const req = httpMock.expectOne(`${API_URL}/defense-request/${requestId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should get my soutenances', () => {
      const mockResponse: SoutenanceResponse[] = [
        {
          id: 1,
          titrethese: 'Test Thesis 1',
          statut: SoutenanceStatus.SOUMISE,
          doctorant: {
            id: 1,
            FirstName: 'John',
            LastName: 'Doe',
            email: 'john@test.com',
            phoneNumber: '0123456789',
            adresse: 'Test Address',
            ville: 'Test City',
            pays: 'France',
            enabled: true,
            roles: []
          },
          directeur: {
            id: 2,
            FirstName: 'Jane',
            LastName: 'Smith',
            email: 'jane@test.com',
            phoneNumber: '0123456789',
            adresse: 'Test Address',
            ville: 'Test City',
            pays: 'France',
            enabled: true,
            roles: []
          },
          jury: [],
          
          prerequis: {
            prerequisRemplis: true,
            publicationsValides: true,
            heuresFormationValides: true,
            dureeDoctoratValide: true,
            documentsCompletsValides: true,
            details: []
          }
        }
      ];

      service.getMySoutenances().subscribe(response => {
        expect(response).toEqual(mockResponse);
        expect(response.length).toBe(1);
      });

      const req = httpMock.expectOne(`${SOUTENANCE_API_URL}/me`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should update demande soutenance', () => {
      const soutenanceId = 1;
      const mockRequest: SoutenanceRequest = {
        titrethese: 'Updated Thesis Title',
        jury: []
      };

      const mockResponse: SoutenanceResponse = {
        id: soutenanceId,
        titrethese: 'Updated Thesis Title',
        statut: SoutenanceStatus.BROUILLON,
        doctorant: {
          id: 1,
          FirstName: 'John',
          LastName: 'Doe',
          email: 'john@test.com',
          phoneNumber: '0123456789',
          adresse: 'Test Address',
          ville: 'Test City',
          pays: 'France',
          enabled: true,
          roles: []
        },
        directeur: {
          id: 2,
          FirstName: 'Jane',
          LastName: 'Smith',
          email: 'jane@test.com',
          phoneNumber: '0123456789',
          adresse: 'Test Address',
          ville: 'Test City',
          pays: 'France',
          enabled: true,
          roles: []
        },
        jury: [],
        
        prerequis: {
          prerequisRemplis: false,
          publicationsValides: false,
          heuresFormationValides: false,
          dureeDoctoratValide: true,
          documentsCompletsValides: false,
          details: []
        }
      };

      service.updateDemandeSoutenance(soutenanceId, mockRequest).subscribe(response => {
        expect(response).toEqual(mockResponse);
        expect(response.titrethese).toBe('Updated Thesis Title');
      });

      const req = httpMock.expectOne(`${SOUTENANCE_API_URL}/${soutenanceId}`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(mockRequest);
      req.flush(mockResponse);
    });
  });

  describe('Prerequis Endpoints', () => {
    it('should check prerequis', () => {
      const doctorantId = 1;
      const mockResponse: PrerequisStatus = {
        prerequisRemplis: true,
        publicationsValides: true,
        heuresFormationValides: true,
        dureeDoctoratValide: true,
        documentsCompletsValides: true,
        details: [
          {
            critere: 'Publications',
            valide: true,
            valeurRequise: '2',
            valeurActuelle: '3'
          },
          {
            critere: 'Formation',
            valide: true,
            valeurRequise: '40',
            valeurActuelle: '45'
          }
        ]
      };

      service.checkPrerequis(doctorantId).subscribe(response => {
        expect(response).toEqual(mockResponse);
        expect(response.prerequisRemplis).toBe(true);
        expect(response.details.length).toBe(2);
      });

      const req = httpMock.expectOne(`${SOUTENANCE_API_URL}/prerequis/${doctorantId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should validate prerequis', () => {
      const soutenanceId = 1;

      service.validatePrerequis(soutenanceId).subscribe(response => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${SOUTENANCE_API_URL}/${soutenanceId}/valider-prerequis`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush({ success: true });
    });
  });

  describe('Jury Endpoints', () => {
    it('should propose jury', () => {
      const soutenanceId = 1;
      const jury: JuryMemberRequest[] = [
        {
          nom: 'Dupont',
          prenom: 'Pierre',
          etablissement: 'Université Test',
          grade: 'Professeur',
          role: JuryRole.PRESIDENT,
          externe: false
        },
        {
          nom: 'Martin',
          prenom: 'Marie',
          etablissement: 'Université Externe',
          grade: 'Maître de conférences',
          role: JuryRole.RAPPORTEUR,
          externe: true
        }
      ];

      service.proposeJury(soutenanceId, jury).subscribe(response => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${SOUTENANCE_API_URL}/${soutenanceId}/jury`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ jury });
      req.flush({ success: true });
    });

    it('should validate jury', () => {
      const soutenanceId = 1;
      const validation = {
        valide: true,
        commentaire: 'Jury composition approved'
      };

      service.validerJury(soutenanceId, validation).subscribe(response => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${SOUTENANCE_API_URL}/${soutenanceId}/valider-jury`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(validation);
      req.flush({ success: true });
    });
  });

  describe('Planning Endpoints', () => {
    it('should planifier soutenance', () => {
      const soutenanceId = 1;
      const planning = {
        dateSoutenance: new Date(),
        lieuSoutenance: 'Amphithéâtre A'
      };

      service.planifierSoutenance(soutenanceId, planning).subscribe(response => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${SOUTENANCE_API_URL}/${soutenanceId}/planifier`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(planning);
      req.flush({ success: true });
    });

    it('should authorize soutenance', () => {
      const soutenanceId = 1;
      const mockResponse: SoutenanceResponse = {
        id: soutenanceId,
        titrethese: 'Test Thesis',
        statut: SoutenanceStatus.AUTORISEE,
        doctorant: {
          id: 1,
          FirstName: 'John',
          LastName: 'Doe',
          email: 'john@test.com',
          phoneNumber: '0123456789',
          adresse: 'Test Address',
          ville: 'Test City',
          pays: 'France',
          enabled: true,
          roles: []
        },
        directeur: {
          id: 2,
          FirstName: 'Jane',
          LastName: 'Smith',
          email: 'jane@test.com',
          phoneNumber: '0123456789',
          adresse: 'Test Address',
          ville: 'Test City',
          pays: 'France',
          enabled: true,
          roles: []
        },
        jury: [],
        
        prerequis: {
          prerequisRemplis: true,
          publicationsValides: true,
          heuresFormationValides: true,
          dureeDoctoratValide: true,
          documentsCompletsValides: true,
          details: []
        }
      };

      service.autoriserSoutenance(soutenanceId).subscribe(response => {
        expect(response).toEqual(mockResponse);
        expect(response.statut).toBe(SoutenanceStatus.AUTORISEE);
      });

      const req = httpMock.expectOne(`${SOUTENANCE_API_URL}/${soutenanceId}/autoriser`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush(mockResponse);
    });

    it('should reject soutenance', () => {
      const soutenanceId = 1;
      const motif = 'Documents incomplets';
      const mockResponse: SoutenanceResponse = {
        id: soutenanceId,
        titrethese: 'Test Thesis',
        statut: SoutenanceStatus.REJETEE,
        doctorant: {
          id: 1,
          FirstName: 'John',
          LastName: 'Doe',
          email: 'john@test.com',
          phoneNumber: '0123456789',
          adresse: 'Test Address',
          ville: 'Test City',
          pays: 'France',
          enabled: true,
          roles: []
        },
        directeur: {
          id: 2,
          FirstName: 'Jane',
          LastName: 'Smith',
          email: 'jane@test.com',
          phoneNumber: '0123456789',
          adresse: 'Test Address',
          ville: 'Test City',
          pays: 'France',
          enabled: true,
          roles: []
        },
        jury: [],
        
        prerequis: {
          prerequisRemplis: false,
          publicationsValides: false,
          heuresFormationValides: false,
          dureeDoctoratValide: true,
          documentsCompletsValides: false,
          details: []
        }
      };

      service.rejeterSoutenance(soutenanceId, motif).subscribe(response => {
        expect(response).toEqual(mockResponse);
        expect(response.statut).toBe(SoutenanceStatus.REJETEE);
      });

      const req = httpMock.expectOne(`${SOUTENANCE_API_URL}/${soutenanceId}/rejeter`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ motif });
      req.flush(mockResponse);
    });
  });

  describe('Utility Methods', () => {
    it('should get status label', () => {
      expect(service.getStatusLabel(SoutenanceStatus.BROUILLON)).toBe('Brouillon');
      expect(service.getStatusLabel(SoutenanceStatus.SOUMISE)).toBe('Soumise');
      expect(service.getStatusLabel(SoutenanceStatus.AUTORISEE)).toBe('Autorisée');
      expect(service.getStatusLabel(SoutenanceStatus.REJETEE)).toBe('Rejetée');
      expect(service.getStatusLabel(SoutenanceStatus.SOUTENUE)).toBe('Soutenue');
    });

    it('should get status color', () => {
      expect(service.getStatusColor(SoutenanceStatus.BROUILLON)).toBe('gray');
      expect(service.getStatusColor(SoutenanceStatus.SOUMISE)).toBe('blue');
      expect(service.getStatusColor(SoutenanceStatus.AUTORISEE)).toBe('green');
      expect(service.getStatusColor(SoutenanceStatus.REJETEE)).toBe('red');
      expect(service.getStatusColor(SoutenanceStatus.SOUTENUE)).toBe('purple');
    });

    it('should check if prerequis are met', () => {
      const prerequisMet: PrerequisStatus = {
        prerequisRemplis: true,
        publicationsValides: true,
        heuresFormationValides: true,
        dureeDoctoratValide: true,
        documentsCompletsValides: true,
        details: []
      };

      const prerequisNotMet: PrerequisStatus = {
        prerequisRemplis: false,
        publicationsValides: false,
        heuresFormationValides: true,
        dureeDoctoratValide: true,
        documentsCompletsValides: false,
        details: []
      };

      expect(service.arePrerequisMet(prerequisMet)).toBe(true);
      expect(service.arePrerequisMet(prerequisNotMet)).toBe(false);
    });

    it('should get missing prerequis', () => {
      const prerequis: PrerequisStatus = {
        prerequisRemplis: false,
        publicationsValides: false,
        heuresFormationValides: true,
        dureeDoctoratValide: true,
        documentsCompletsValides: false,
        details: [
          {
            critere: 'Publications',
            valide: false,
            valeurRequise: '2',
            valeurActuelle: '1'
          },
          {
            critere: 'Formation',
            valide: true,
            valeurRequise: '40',
            valeurActuelle: '45'
          },
          {
            critere: 'Documents',
            valide: false,
            valeurRequise: '5',
            valeurActuelle: '3'
          }
        ]
      };

      const missing = service.getMissingPrerequis(prerequis);
      expect(missing).toEqual(['Publications', 'Documents']);
      expect(missing.length).toBe(2);
    });

    it('should validate jury composition', () => {
      const validJury: JuryMember[] = [
        {
          id: 1,
          nom: 'Dupont',
          prenom: 'Pierre',
          etablissement: 'Université Test',
          grade: 'Professeur',
          role: JuryRole.PRESIDENT,
          externe: false
        },
        {
          id: 2,
          nom: 'Martin',
          prenom: 'Marie',
          etablissement: 'Université Test',
          grade: 'Maître de conférences',
          role: JuryRole.RAPPORTEUR,
          externe: false
        },
        {
          id: 3,
          nom: 'Durand',
          prenom: 'Paul',
          etablissement: 'Université Externe',
          grade: 'Professeur',
          role: JuryRole.EXAMINATEUR,
          externe: true
        }
      ];

      const invalidJury: JuryMember[] = [
        {
          id: 1,
          nom: 'Dupont',
          prenom: 'Pierre',
          etablissement: 'Université Test',
          grade: 'Professeur',
          role: JuryRole.EXAMINATEUR,
          externe: false
        }
      ];

      const validResult = service.validateJuryComposition(validJury);
      expect(validResult.valid).toBe(true);
      expect(validResult.errors.length).toBe(0);

      const invalidResult = service.validateJuryComposition(invalidJury);
      expect(invalidResult.valid).toBe(false);
      expect(invalidResult.errors.length).toBeGreaterThan(0);
      expect(invalidResult.errors).toContain('Le jury doit comporter au moins 3 membres');
      expect(invalidResult.errors).toContain('Le jury doit avoir un président');
      expect(invalidResult.errors).toContain('Le jury doit avoir au moins un rapporteur');
    });
  });

  describe('Error Handling', () => {
    it('should handle HTTP 400 error', () => {
      const mockRequest: SoutenanceRequest = {
        titrethese: '',
        jury: []
      };

      service.createDemandeSoutenance(mockRequest).subscribe({
        next: () => fail('Should have failed'),
        error: (error) => {
          expect(error.status).toBe(400);
          expect(error.error.message).toBe('Invalid soutenance data');
        }
      });

      const req = httpMock.expectOne(SOUTENANCE_API_URL);
      req.flush({ message: 'Invalid soutenance data' }, { status: 400, statusText: 'Bad Request' });
    });

    it('should handle HTTP 404 error', () => {
      const soutenanceId = 999;

      service.getSoutenance(soutenanceId).subscribe({
        next: () => fail('Should have failed'),
        error: (error) => {
          expect(error.status).toBe(404);
          expect(error.error.message).toBe('Soutenance not found');
        }
      });

      const req = httpMock.expectOne(`${SOUTENANCE_API_URL}/${soutenanceId}`);
      req.flush({ message: 'Soutenance not found' }, { status: 404, statusText: 'Not Found' });
    });

    it('should handle HTTP 403 error for unauthorized access', () => {
      const doctorantId = 1;

      service.checkPrerequis(doctorantId).subscribe({
        next: () => fail('Should have failed'),
        error: (error) => {
          expect(error.status).toBe(403);
          expect(error.error.message).toBe('Access denied');
        }
      });

      const req = httpMock.expectOne(`${SOUTENANCE_API_URL}/prerequis/${doctorantId}`);
      req.flush({ message: 'Access denied' }, { status: 403, statusText: 'Forbidden' });
    });
  });
});
