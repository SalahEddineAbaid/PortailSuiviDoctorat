import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';

import { ParametrageComponent } from './parametrage.component';
import { ParametrageService } from '../../../core/services/parametrage.service';
import { 
  SeuilConfiguration, 
  DocumentTypeConfiguration, 
  NotificationConfiguration,
  SystemConfiguration,
  ConfigurationCategory,
  ConfigurationType,
  SeuilCategory,
  DocumentCategory,
  NotificationDestinataire
} from '../../../core/models/parametrage.model';

describe('ParametrageComponent', () => {
  let component: ParametrageComponent;
  let fixture: ComponentFixture<ParametrageComponent>;
  let parametrageService: jasmine.SpyObj<ParametrageService>;

  const mockSeuils: SeuilConfiguration[] = [
    {
      id: 1,
      nom: 'Durée maximale doctorat',
      valeur: 6,
      unite: 'années',
      description: 'Durée maximale autorisée pour un doctorat',
      category: SeuilCategory.DUREE_DOCTORAT
    }
  ];

  const mockDocumentTypes: DocumentTypeConfiguration[] = [
    {
      id: 1,
      type: 'CV',
      nom: 'Curriculum Vitae',
      obligatoire: true,
      formatAutorise: ['PDF'],
      tailleMaxMo: 5,
      description: 'CV du doctorant',
      category: DocumentCategory.INSCRIPTION
    }
  ];

  const mockNotifications: NotificationConfiguration[] = [
    {
      id: 1,
      type: 'INSCRIPTION_SOUMISE',
      nom: 'Inscription soumise',
      template: 'Votre inscription a été soumise',
      actif: true,
      destinataires: [NotificationDestinataire.DOCTORANT]
    }
  ];

  const mockGeneralConfigs: SystemConfiguration[] = [
    {
      id: 1,
      category: ConfigurationCategory.GENERAL,
      key: 'MAINTENANCE_MODE',
      value: 'false',
      description: 'Mode maintenance',
      type: ConfigurationType.BOOLEAN,
      updatedAt: new Date(),
      updatedBy: 1
    }
  ];

  beforeEach(async () => {
    const parametrageServiceSpy = jasmine.createSpyObj('ParametrageService', [
      'getAllSeuils',
      'getAllDocumentTypes', 
      'getAllNotificationConfigs',
      'getConfigurationsByCategory',
      'updateSeuils',
      'updateDocumentTypes',
      'updateNotificationConfigs',
      'updateConfigurations',
      'createDocumentType',
      'deleteDocumentType',
      'toggleNotificationConfig',
      'resetToDefaults',
      'exportConfiguration',
      'importConfiguration'
    ]);

    await TestBed.configureTestingModule({
      imports: [
        ParametrageComponent,
        ReactiveFormsModule,
        HttpClientTestingModule
      ],
      providers: [
        { provide: ParametrageService, useValue: parametrageServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ParametrageComponent);
    component = fixture.componentInstance;
    parametrageService = TestBed.inject(ParametrageService) as jasmine.SpyObj<ParametrageService>;

    // Setup default mock responses
    parametrageService.getAllSeuils.and.returnValue(of({ data: mockSeuils, success: true, message: '', timestamp: new Date() }));
    parametrageService.getAllDocumentTypes.and.returnValue(of({ data: mockDocumentTypes, success: true, message: '', timestamp: new Date() }));
    parametrageService.getAllNotificationConfigs.and.returnValue(of({ data: mockNotifications, success: true, message: '', timestamp: new Date() }));
    parametrageService.getConfigurationsByCategory.and.returnValue(of({ data: mockGeneralConfigs, success: true, message: '', timestamp: new Date() }));
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load seuils on init', () => {
    fixture.detectChanges();
    
    expect(parametrageService.getAllSeuils).toHaveBeenCalled();
    expect(component.seuils).toEqual(mockSeuils);
  });

  it('should set active tab', () => {
    component.setActiveTab('documents');
    
    expect(component.activeTab).toBe('documents');
    expect(parametrageService.getAllDocumentTypes).toHaveBeenCalled();
  });

  it('should build seuils form correctly', () => {
    component.seuils = mockSeuils;
    component['buildSeuilsForm']();
    
    expect(component.seuilsForm.get('seuil_1')?.value).toBe(6);
  });

  it('should save seuils', () => {
    component.seuils = mockSeuils;
    component['buildSeuilsForm']();
    parametrageService.updateSeuils.and.returnValue(of({ data: mockSeuils, success: true, message: '', timestamp: new Date() }));
    
    component.saveSeuils();
    
    expect(parametrageService.updateSeuils).toHaveBeenCalled();
  });

  it('should add document type', () => {
    component.addDocumentType();
    
    expect(component.showDocumentTypeModal).toBe(true);
    expect(component.editingDocumentType).toBeNull();
  });

  it('should edit document type', () => {
    const docType = mockDocumentTypes[0];
    
    component.editDocumentType(docType);
    
    expect(component.showDocumentTypeModal).toBe(true);
    expect(component.editingDocumentType).toBe(docType);
  });

  it('should close document type modal', () => {
    component.showDocumentTypeModal = true;
    component.editingDocumentType = mockDocumentTypes[0];
    
    component.closeDocumentTypeModal();
    
    expect(component.showDocumentTypeModal).toBe(false);
    expect(component.editingDocumentType).toBeNull();
  });

  it('should toggle notification', () => {
    const mockEvent = { target: { checked: false } };
    parametrageService.toggleNotificationConfig.and.returnValue(of({ 
      data: { ...mockNotifications[0], actif: false }, 
      success: true, 
      message: '', 
      timestamp: new Date() 
    }));
    component.notifications = [...mockNotifications];
    
    component.toggleNotification(1, mockEvent);
    
    expect(parametrageService.toggleNotificationConfig).toHaveBeenCalledWith(1, false);
  });

  it('should export configuration', () => {
    const mockBlob = new Blob(['test'], { type: 'application/json' });
    parametrageService.exportConfiguration.and.returnValue(of(mockBlob));
    
    spyOn(window.URL, 'createObjectURL').and.returnValue('mock-url');
    spyOn(window.URL, 'revokeObjectURL');
    
    component.exportConfiguration();
    
    expect(parametrageService.exportConfiguration).toHaveBeenCalled();
  });
});