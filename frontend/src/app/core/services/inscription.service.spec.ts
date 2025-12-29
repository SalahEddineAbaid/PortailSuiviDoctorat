import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { InscriptionService } from './inscription.service';
import { environment } from '../../environments/environment';
import {
  InscriptionRequest,
  InscriptionResponse,
  CampagneRequest,
  CampagneResponse,
  ValidationRequest,
  InscriptionStatus,
  TypeInscription
} from '../models/inscription.model';

describe('InscriptionService', () => {
  let service: InscriptionService;
  let httpMock: HttpTestingController;
  const API_URL = `${environment.apiUrl}/inscriptions`;
  const CAMPAGNE_API_URL = `${environment.apiUrl}/campagnes`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        InscriptionService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(InscriptionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Inscription Endpoints', () => {
    it('should create inscription', () => {
      const mockRequest: InscriptionRequest = {
        sujetThese: 'Test Subject',
        laboratoire: 'Test Lab',
        specialite: 'Test Specialty',
        directeurId: 1,
        campagneId: 1
      };

      const mockResponse: InscriptionResponse = {
        id: 1,
        sujetThese: 'Test Subject',
        laboratoire: 'Test Lab',
        specialite: 'Test Specialty',
        statut: InscriptionStatus.BROUILLON,
        dateInscription: new Date(),
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
        campagne: {
          id: 1,
          nom: 'Test Campaign',
          anneeUniversitaire: '2024-2025',
          dateOuverture: new Date(),
          dateFermeture: new Date(),
          active: true,
          typeInscription: TypeInscription.PREMIERE
        },
        documents: []
      };

      service.createInscription(mockRequest).subscribe(response => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(API_URL);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(mockRequest);
      req.flush(mockResponse);
    });

    it('should submit inscription', () => {
      const inscriptionId = 1;
      const mockResponse: InscriptionResponse = {
        id: inscriptionId,
        sujetThese: 'Test Subject',
        laboratoire: 'Test Lab',
        specialite: 'Test Specialty',
        statut: InscriptionStatus.SOUMISE,
        dateInscription: new Date(),
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
        campagne: {
          id: 1,
          nom: 'Test Campaign',
          anneeUniversitaire: '2024-2025',
          dateOuverture: new Date(),
          dateFermeture: new Date(),
          active: true,
          typeInscription: TypeInscription.PREMIERE
        },
        documents: []
      };

      service.submitInscription(inscriptionId).subscribe(response => {
        expect(response).toEqual(mockResponse);
        expect(response.statut).toBe(InscriptionStatus.SOUMISE);
      });

      const req = httpMock.expectOne(`${API_URL}/${inscriptionId}/soumettre`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush(mockResponse);
    });

    it('should get inscription by id', () => {
      const inscriptionId = 1;
      const mockResponse: InscriptionResponse = {
        id: inscriptionId,
        sujetThese: 'Test Subject',
        laboratoire: 'Test Lab',
        specialite: 'Test Specialty',
        statut: InscriptionStatus.VALIDEE,
        dateInscription: new Date(),
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
        campagne: {
          id: 1,
          nom: 'Test Campaign',
          anneeUniversitaire: '2024-2025',
          dateOuverture: new Date(),
          dateFermeture: new Date(),
          active: true,
          typeInscription: TypeInscription.PREMIERE
        },
        documents: []
      };

      service.getInscription(inscriptionId).subscribe(response => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${API_URL}/${inscriptionId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should get my inscriptions', () => {
      const mockResponse: InscriptionResponse[] = [
        {
          id: 1,
          sujetThese: 'Test Subject 1',
          laboratoire: 'Test Lab 1',
          specialite: 'Test Specialty 1',
          statut: InscriptionStatus.VALIDEE,
          dateInscription: new Date(),
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
          campagne: {
            id: 1,
            nom: 'Test Campaign',
            anneeUniversitaire: '2024-2025',
            dateOuverture: new Date(),
            dateFermeture: new Date(),
            active: true,
            typeInscription: TypeInscription.PREMIERE
          },
          documents: []
        }
      ];

      service.getMyInscriptions().subscribe(response => {
        expect(response).toEqual(mockResponse);
        expect(response.length).toBe(1);
      });

      const req = httpMock.expectOne(`${API_URL}/me`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should validate inscription by directeur', () => {
      const inscriptionId = 1;
      const validation: ValidationRequest = {
        valide: true,
        commentaire: 'Approved by director'
      };

      const mockResponse: InscriptionResponse = {
        id: inscriptionId,
        sujetThese: 'Test Subject',
        laboratoire: 'Test Lab',
        specialite: 'Test Specialty',
        statut: InscriptionStatus.EN_COURS_VALIDATION,
        dateInscription: new Date(),
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
        campagne: {
          id: 1,
          nom: 'Test Campaign',
          anneeUniversitaire: '2024-2025',
          dateOuverture: new Date(),
          dateFermeture: new Date(),
          active: true,
          typeInscription: TypeInscription.PREMIERE
        },
        documents: []
      };

      service.validerParDirecteur(inscriptionId, validation).subscribe(response => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${API_URL}/${inscriptionId}/valider-directeur`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(validation);
      req.flush(mockResponse);
    });
  });

  describe('Campagne Endpoints', () => {
    it('should create campagne', () => {
      const mockRequest: CampagneRequest = {
        nom: 'Test Campaign',
        anneeUniversitaire: '2024-2025',
        dateOuverture: new Date(),
        dateFermeture: new Date(),
        typeInscription: TypeInscription.PREMIERE
      };

      const mockResponse: CampagneResponse = {
        id: 1,
        nom: 'Test Campaign',
        anneeUniversitaire: '2024-2025',
        dateOuverture: new Date(),
        dateFermeture: new Date(),
        dateCreation: new Date(),
        active: true,
        typeInscription: TypeInscription.PREMIERE
      };

      service.createCampagne(mockRequest).subscribe(response => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(CAMPAGNE_API_URL);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(mockRequest);
      req.flush(mockResponse);
    });

    it('should get all campagnes', () => {
      const mockResponse: CampagneResponse[] = [
        {
          id: 1,
          nom: 'Test Campaign 1',
          anneeUniversitaire: '2024-2025',
          dateOuverture: new Date(),
          dateFermeture: new Date(),
          dateCreation: new Date(),
          active: true,
          typeInscription: TypeInscription.PREMIERE
        },
        {
          id: 2,
          nom: 'Test Campaign 2',
          anneeUniversitaire: '2023-2024',
          dateOuverture: new Date(),
          dateFermeture: new Date(),
          dateCreation: new Date(),
          active: false,
          typeInscription: TypeInscription.REINSCRIPTION
        }
      ];

      service.getAllCampagnes().subscribe(response => {
        expect(response).toEqual(mockResponse);
        expect(response.length).toBe(2);
      });

      const req = httpMock.expectOne(CAMPAGNE_API_URL);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should get active campagne', () => {
      const mockResponse: CampagneResponse = {
        id: 1,
        nom: 'Active Campaign',
        anneeUniversitaire: '2024-2025',
        dateOuverture: new Date(),
        dateFermeture: new Date(),
        dateCreation: new Date(),
        active: true,
        typeInscription: TypeInscription.PREMIERE
      };

      service.getCampagneActive().subscribe(response => {
        expect(response).toEqual(mockResponse);
        expect(response?.active).toBe(true);
      });

      const req = httpMock.expectOne(`${CAMPAGNE_API_URL}/active`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should close campagne', () => {
      const campagneId = 1;
      const mockResponse: CampagneResponse = {
        id: campagneId,
        nom: 'Test Campaign',
        anneeUniversitaire: '2024-2025',
        dateOuverture: new Date(),
        dateFermeture: new Date(),
        dateCreation: new Date(),
        active: false,
        typeInscription: TypeInscription.PREMIERE
      };

      service.fermerCampagne(campagneId).subscribe(response => {
        expect(response).toEqual(mockResponse);
        expect(response.active).toBe(false);
      });

      const req = httpMock.expectOne(`${CAMPAGNE_API_URL}/${campagneId}/fermer`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({});
      req.flush(mockResponse);
    });
  });

  describe('Utility Methods', () => {
    it('should check if campagne is open', () => {
      const now = new Date();
      const openCampagne: CampagneResponse = {
        id: 1,
        nom: 'Open Campaign',
        anneeUniversitaire: '2024-2025',
        dateOuverture: new Date(now.getTime() - 86400000), // Yesterday
        dateFermeture: new Date(now.getTime() + 86400000), // Tomorrow
        dateCreation: new Date(),
        active: true,
        typeInscription: TypeInscription.PREMIERE
      };

      const closedCampagne: CampagneResponse = {
        id: 2,
        nom: 'Closed Campaign',
        anneeUniversitaire: '2024-2025',
        dateOuverture: new Date(now.getTime() - 172800000), // 2 days ago
        dateFermeture: new Date(now.getTime() - 86400000), // Yesterday
        dateCreation: new Date(),
        active: true,
        typeInscription: TypeInscription.PREMIERE
      };

      expect(service.isCampagneOuverte(openCampagne)).toBe(true);
      expect(service.isCampagneOuverte(closedCampagne)).toBe(false);
    });

    it('should get status label', () => {
      expect(service.getStatusLabel(InscriptionStatus.BROUILLON)).toBe('Brouillon');
      expect(service.getStatusLabel(InscriptionStatus.SOUMISE)).toBe('Soumise');
      expect(service.getStatusLabel(InscriptionStatus.VALIDEE)).toBe('Validée');
      expect(service.getStatusLabel(InscriptionStatus.REJETEE)).toBe('Rejetée');
    });

    it('should get status color', () => {
      expect(service.getStatusColor(InscriptionStatus.BROUILLON)).toBe('gray');
      expect(service.getStatusColor(InscriptionStatus.SOUMISE)).toBe('blue');
      expect(service.getStatusColor(InscriptionStatus.VALIDEE)).toBe('green');
      expect(service.getStatusColor(InscriptionStatus.REJETEE)).toBe('red');
    });
  });

  describe('Error Handling', () => {
    it('should handle HTTP 400 error', () => {
      const mockRequest: InscriptionRequest = {
        sujetThese: '',
        laboratoire: '',
        specialite: '',
        directeurId: 0,
        campagneId: 0
      };

      service.createInscription(mockRequest).subscribe({
        next: () => fail('Should have failed'),
        error: (error) => {
          expect(error.status).toBe(400);
          expect(error.error.message).toBe('Invalid data');
        }
      });

      const req = httpMock.expectOne(API_URL);
      req.flush({ message: 'Invalid data' }, { status: 400, statusText: 'Bad Request' });
    });

    it('should handle HTTP 404 error', () => {
      const inscriptionId = 999;

      service.getInscription(inscriptionId).subscribe({
        next: () => fail('Should have failed'),
        error: (error) => {
          expect(error.status).toBe(404);
          expect(error.error.message).toBe('Inscription not found');
        }
      });

      const req = httpMock.expectOne(`${API_URL}/${inscriptionId}`);
      req.flush({ message: 'Inscription not found' }, { status: 404, statusText: 'Not Found' });
    });

    it('should handle HTTP 500 error', () => {
      service.getMyInscriptions().subscribe({
        next: () => fail('Should have failed'),
        error: (error) => {
          expect(error.status).toBe(500);
          expect(error.error.message).toBe('Internal server error');
        }
      });

      const req = httpMock.expectOne(`${API_URL}/me`);
      req.flush({ message: 'Internal server error' }, { status: 500, statusText: 'Internal Server Error' });
    });
  });
});
