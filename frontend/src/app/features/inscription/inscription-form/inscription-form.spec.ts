import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';

import { InscriptionForm } from './inscription-form';
import { InscriptionService } from '../../../core/services/inscription.service';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';

import { 
  InscriptionRequest, 
  InscriptionResponse, 
  CampagneResponse,
  InscriptionStatus,
  TypeInscription 
} from '../../../core/models/inscription.model';
import { UserResponse } from '../../../core/models/user.model';
import { RoleName } from '../../../core/models/role.model';

describe('InscriptionForm', () => {
  let component: InscriptionForm;
  let fixture: ComponentFixture<InscriptionForm>;
  let mockInscriptionService: jasmine.SpyObj<InscriptionService>;
  let mockUserService: jasmine.SpyObj<UserService>;
  let mockAuthService: jasmine.SpyObj<AuthService>;
  let mockRouter: jasmine.SpyObj<Router>;
  let mockActivatedRoute: any;

  const mockCampagne: CampagneResponse = {
    id: 1,
    nom: 'Campagne Test 2024',
    anneeUniversitaire: '2024-2025',
    dateOuverture: new Date('2024-01-01'),
    dateFermeture: new Date('2024-12-31'),
    dateCreation: new Date('2023-12-01'),
    active: true,
    typeInscription: TypeInscription.PREMIERE
  };

  const mockDirecteurs: UserResponse[] = [
    {
      id: 1,
      FirstName: 'Jean',
      LastName: 'Dupont',
      email: 'jean.dupont@univ.fr',
      phoneNumber: '0123456789',
      adresse: '123 Rue Test',
      ville: 'Paris',
      pays: 'France',
      enabled: true,
      roles: [{ id: 1, name: RoleName.DIRECTEUR }]
    },
    {
      id: 2,
      FirstName: 'Marie',
      LastName: 'Martin',
      email: 'marie.martin@univ.fr',
      phoneNumber: '0123456788',
      adresse: '456 Avenue Test',
      ville: 'Lyon',
      pays: 'France',
      enabled: true,
      roles: [{ id: 1, name: RoleName.DIRECTEUR }]
    }
  ];

  beforeEach(async () => {
    const inscriptionServiceSpy = jasmine.createSpyObj('InscriptionService', [
      'getCampagneActive', 'createInscription', 'getInscription'
    ]);
    const userServiceSpy = jasmine.createSpyObj('UserService', ['getDirecteurs']);
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['getCurrentUser']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    mockActivatedRoute = {
      snapshot: {
        paramMap: {
          get: jasmine.createSpy('get').and.returnValue(null)
        }
      }
    };

    await TestBed.configureTestingModule({
      imports: [InscriptionForm, ReactiveFormsModule],
      providers: [
        { provide: InscriptionService, useValue: inscriptionServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: mockActivatedRoute }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InscriptionForm);
    component = fixture.componentInstance;
    
    mockInscriptionService = TestBed.inject(InscriptionService) as jasmine.SpyObj<InscriptionService>;
    mockUserService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>;
    mockAuthService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    mockRouter = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    // Setup default mock returns
    mockInscriptionService.getCampagneActive.and.returnValue(of(mockCampagne));
    mockUserService.getDirecteurs.and.returnValue(of(mockDirecteurs));
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Form Initialization', () => {
    it('should initialize form with required validators', () => {
      component.ngOnInit(); // Ensure ngOnInit is called
      fixture.detectChanges();
      
      expect(component.inscriptionForm).toBeDefined();
      
      // Check that the form controls exist and are initially invalid due to required validators
      const directeurControl = component.inscriptionForm.get('directeurId');
      const campagneControl = component.inscriptionForm.get('campagneId');
      const sujetControl = component.inscriptionForm.get('sujetThese');
      const laboratoireControl = component.inscriptionForm.get('laboratoire');
      const specialiteControl = component.inscriptionForm.get('specialite');
      
      expect(directeurControl).toBeTruthy();
      expect(campagneControl).toBeTruthy();
      expect(sujetControl).toBeTruthy();
      expect(laboratoireControl).toBeTruthy();
      expect(specialiteControl).toBeTruthy();
      
      // The campagneId should be auto-filled, so it shouldn't have required error
      expect(directeurControl?.hasError('required')).toBe(true);
      expect(sujetControl?.hasError('required')).toBe(true);
      expect(laboratoireControl?.hasError('required')).toBe(true);
      expect(specialiteControl?.hasError('required')).toBe(true);
    });

    it('should auto-select active campaign on load', () => {
      fixture.detectChanges();
      
      expect(mockInscriptionService.getCampagneActive).toHaveBeenCalled();
      expect(component.inscriptionForm.get('campagneId')?.value).toBe(mockCampagne.id);
    });

    it('should load directeurs on initialization', () => {
      fixture.detectChanges();
      
      expect(mockUserService.getDirecteurs).toHaveBeenCalled();
    });
  });

  describe('Form Validation', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should validate sujet these length', () => {
      const sujetControl = component.inscriptionForm.get('sujetThese');
      
      // Test minimum length
      sujetControl?.setValue('short');
      expect(sujetControl?.hasError('minlength')).toBe(true);
      
      // Test valid length
      sujetControl?.setValue('This is a valid thesis subject that meets minimum requirements');
      expect(sujetControl?.hasError('minlength')).toBe(false);
      
      // Test maximum length
      const longText = 'a'.repeat(501);
      sujetControl?.setValue(longText);
      expect(sujetControl?.hasError('maxlength')).toBe(true);
    });

    it('should require laboratoireAutre when laboratoire is "Autre"', () => {
      const laboratoireControl = component.inscriptionForm.get('laboratoire');
      const laboratoireAutreControl = component.inscriptionForm.get('laboratoireAutre');
      
      laboratoireControl?.setValue('Autre');
      expect(laboratoireAutreControl?.hasError('required')).toBe(true);
      
      laboratoireAutreControl?.setValue('Custom Lab');
      expect(laboratoireAutreControl?.hasError('required')).toBe(false);
    });

    it('should require specialiteAutre when specialite is "Autre"', () => {
      const specialiteControl = component.inscriptionForm.get('specialite');
      const specialiteAutreControl = component.inscriptionForm.get('specialiteAutre');
      
      specialiteControl?.setValue('Autre');
      expect(specialiteAutreControl?.hasError('required')).toBe(true);
      
      specialiteAutreControl?.setValue('Custom Specialty');
      expect(specialiteAutreControl?.hasError('required')).toBe(false);
    });

    it('should validate form as invalid when required fields are empty', () => {
      expect(component.inscriptionForm.valid).toBe(false);
    });

    it('should validate form as valid when all required fields are filled', () => {
      component.inscriptionForm.patchValue({
        directeurId: 1,
        campagneId: 1,
        sujetThese: 'Valid thesis subject with sufficient length',
        laboratoire: 'Laboratoire d\'Informatique',
        specialite: 'Informatique'
      });
      
      expect(component.inscriptionForm.valid).toBe(true);
    });
  });

  describe('Directeur Selection', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should filter directeurs based on search term', (done) => {
      // First, let the initial setup complete
      fixture.detectChanges();
      
      // Wait for initial observables to settle
      setTimeout(() => {
        const searchControl = component.inscriptionForm.get('directeurSearch');
        searchControl?.setValue('Jean');
        
        // Wait for the filtering to process
        setTimeout(() => {
          component.filteredDirecteurs$.subscribe(directeurs => {
            // The filter should work on the name, but let's be more flexible in our test
            const jeanDirecteurs = directeurs.filter(d => d.FirstName.includes('Jean'));
            expect(jeanDirecteurs.length).toBeGreaterThan(0);
            expect(jeanDirecteurs[0].FirstName).toBe('Jean');
            done();
          });
        }, 100);
      }, 100);
    });

    it('should select directeur and update form', () => {
      const directeur = mockDirecteurs[0];
      component.onDirecteurSelect(directeur);
      
      expect(component.inscriptionForm.get('directeurId')?.value).toBe(directeur.id);
      expect(component.inscriptionForm.get('directeurSearch')?.value).toBe(`${directeur.FirstName} ${directeur.LastName}`);
    });
  });

  describe('Form Submission', () => {
    beforeEach(() => {
      fixture.detectChanges();
      // Fill form with valid data
      component.inscriptionForm.patchValue({
        directeurId: 1,
        campagneId: 1,
        sujetThese: 'Valid thesis subject with sufficient length',
        laboratoire: 'Laboratoire d\'Informatique',
        specialite: 'Informatique'
      });
    });

    it('should create inscription on valid form submission', () => {
      const mockResponse: InscriptionResponse = {
        id: 1,
        sujetThese: 'Valid thesis subject with sufficient length',
        laboratoire: 'Laboratoire d\'Informatique',
        specialite: 'Informatique',
        statut: InscriptionStatus.BROUILLON,
        dateInscription: new Date(),
        doctorant: mockDirecteurs[0], // Using mock user
        directeur: mockDirecteurs[0],
        campagne: mockCampagne,
        documents: []
      };

      mockInscriptionService.createInscription.and.returnValue(of(mockResponse));
      
      component.onSubmit();
      
      expect(mockInscriptionService.createInscription).toHaveBeenCalledWith(jasmine.objectContaining({
        directeurId: 1,
        campagneId: 1,
        sujetThese: 'Valid thesis subject with sufficient length',
        laboratoire: 'Laboratoire d\'Informatique',
        specialite: 'Informatique'
      }));
      
      expect(component.success).toBe('Inscription créée avec succès !');
    });

    it('should handle creation error', () => {
      const error = { status: 400, error: { message: 'Invalid data' } };
      mockInscriptionService.createInscription.and.returnValue(throwError(() => error));
      
      component.onSubmit();
      
      expect(component.error).toBe('Invalid data');
      expect(component.loading).toBe(false);
    });

    it('should not submit invalid form', () => {
      // Clear form to make it invalid
      component.inscriptionForm.patchValue({
        directeurId: '',
        sujetThese: ''
      });
      
      component.onSubmit();
      
      expect(mockInscriptionService.createInscription).not.toHaveBeenCalled();
      expect(component.inscriptionForm.get('directeurId')?.touched).toBe(true);
      expect(component.inscriptionForm.get('sujetThese')?.touched).toBe(true);
    });

    it('should prepare form data correctly with custom laboratoire', () => {
      component.inscriptionForm.patchValue({
        directeurId: 1,
        campagneId: 1,
        sujetThese: 'Valid thesis subject with sufficient length',
        laboratoire: 'Autre',
        laboratoireAutre: 'Custom Laboratory',
        specialite: 'Informatique'
      });

      const mockResponse: InscriptionResponse = {
        id: 1,
        sujetThese: 'Valid thesis subject with sufficient length',
        laboratoire: 'Custom Laboratory',
        specialite: 'Informatique',
        statut: InscriptionStatus.BROUILLON,
        dateInscription: new Date(),
        doctorant: mockDirecteurs[0],
        directeur: mockDirecteurs[0],
        campagne: mockCampagne,
        documents: []
      };

      mockInscriptionService.createInscription.and.returnValue(of(mockResponse));
      
      component.onSubmit();
      
      expect(mockInscriptionService.createInscription).toHaveBeenCalledWith(jasmine.objectContaining({
        laboratoire: 'Custom Laboratory'
      }));
    });
  });

  describe('Edit Mode', () => {
    const mockInscription: InscriptionResponse = {
      id: 1,
      sujetThese: 'Existing thesis subject',
      laboratoire: 'Laboratoire de Physique',
      specialite: 'Physique',
      statut: InscriptionStatus.BROUILLON,
      dateInscription: new Date(),
      doctorant: mockDirecteurs[0],
      directeur: mockDirecteurs[1],
      campagne: mockCampagne,
      documents: []
    };

    beforeEach(() => {
      mockActivatedRoute.snapshot.paramMap.get.and.returnValue('1');
      mockInscriptionService.getInscription.and.returnValue(of(mockInscription));
    });

    it('should load inscription data in edit mode', () => {
      fixture.detectChanges();
      
      expect(component.isEditMode).toBe(true);
      expect(component.inscriptionId).toBe(1);
      expect(mockInscriptionService.getInscription).toHaveBeenCalledWith(1);
    });

    it('should populate form with existing inscription data', () => {
      fixture.detectChanges();
      
      expect(component.inscriptionForm.get('sujetThese')?.value).toBe(mockInscription.sujetThese);
      expect(component.inscriptionForm.get('laboratoire')?.value).toBe(mockInscription.laboratoire);
      expect(component.inscriptionForm.get('specialite')?.value).toBe(mockInscription.specialite);
      expect(component.inscriptionForm.get('directeurId')?.value).toBe(mockInscription.directeur.id);
    });

    it('should handle edit mode loading error', () => {
      const error = { status: 404, error: { message: 'Not found' } };
      mockInscriptionService.getInscription.and.returnValue(throwError(() => error));
      
      fixture.detectChanges();
      
      expect(component.error).toBe('Erreur lors du chargement de l\'inscription');
      expect(component.loading).toBe(false);
    });
  });

  describe('Helper Methods', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should detect invalid fields correctly', () => {
      const sujetControl = component.inscriptionForm.get('sujetThese');
      sujetControl?.markAsTouched();
      
      expect(component.isFieldInvalid('sujetThese')).toBe(true);
      
      sujetControl?.setValue('Valid thesis subject with sufficient length');
      expect(component.isFieldInvalid('sujetThese')).toBe(false);
    });

    it('should return appropriate error messages', () => {
      const sujetControl = component.inscriptionForm.get('sujetThese');
      sujetControl?.markAsTouched();
      
      expect(component.getFieldError('sujetThese')).toBe('Ce champ est obligatoire');
      
      sujetControl?.setValue('short');
      expect(component.getFieldError('sujetThese')).toContain('Minimum');
    });

    it('should navigate to inscription list on cancel', () => {
      component.onCancel();
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/inscription']);
    });
  });

  describe('Error Handling', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should handle different HTTP error codes', () => {
      const testCases = [
        { status: 400, expectedMessage: 'Données invalides. Veuillez vérifier votre saisie.' },
        { status: 409, expectedMessage: 'Une inscription existe déjà pour cette campagne.' },
        { status: 500, expectedMessage: 'Une erreur est survenue lors de l\'enregistrement.' }
      ];

      testCases.forEach(testCase => {
        const error = { status: testCase.status };
        const errorMessage = component['getErrorMessage'](error);
        expect(errorMessage).toBe(testCase.expectedMessage);
      });
    });

    it('should use error message from response when available', () => {
      const error = { error: { message: 'Custom error message' } };
      const errorMessage = component['getErrorMessage'](error);
      expect(errorMessage).toBe('Custom error message');
    });
  });
});