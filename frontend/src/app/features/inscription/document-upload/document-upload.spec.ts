import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';

import { DocumentUpload, DocumentUploadConfig } from './document-upload';
import { DocumentService } from '../../../core/services/document.service';
import { 
  DocumentType, 
  DocumentResponse, 
  DocumentUploadRequest 
} from '../../../core/models/document.model';

describe('DocumentUpload', () => {
  let component: DocumentUpload;
  let fixture: ComponentFixture<DocumentUpload>;
  let mockDocumentService: jasmine.SpyObj<DocumentService>;

  const mockConfig: DocumentUploadConfig = {
    type: DocumentType.CARTE_IDENTITE,
    label: 'Carte d\'identité',
    required: true,
    maxSizeMB: 5,
    allowedTypes: ['application/pdf', 'image/jpeg', 'image/png'],
    description: 'Téléchargez votre carte d\'identité'
  };

  const mockDocument: DocumentResponse = {
    id: 1,
    nom: 'carte-identite.pdf',
    type: DocumentType.CARTE_IDENTITE,
    taille: 1024000, // 1MB
    dateUpload: new Date(),
    obligatoire: true,
    valide: true
  };

  beforeEach(async () => {
    const documentServiceSpy = jasmine.createSpyObj('DocumentService', [
      'uploadDocument',
      'replaceDocument', 
      'deleteDocument',
      'downloadDocument',
      'validateFileFormat',
      'validateFileSize',
      'formatFileSize',
      'getDocumentTypeIcon'
    ]);

    await TestBed.configureTestingModule({
      imports: [DocumentUpload, FormsModule],
      providers: [
        { provide: DocumentService, useValue: documentServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DocumentUpload);
    component = fixture.componentInstance;
    mockDocumentService = TestBed.inject(DocumentService) as jasmine.SpyObj<DocumentService>;

    // Set required input
    component.config = mockConfig;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Component Initialization', () => {
    it('should validate config on init', () => {
      expect(() => {
        component.config = null as any;
        component.ngOnInit();
      }).toThrowError('DocumentUpload: config is required');
    });

    it('should apply default configuration values', () => {
      const configWithoutDefaults: DocumentUploadConfig = {
        type: DocumentType.CV,
        label: 'CV',
        required: false
      };
      
      component.config = configWithoutDefaults;
      component.ngOnInit();
      
      expect(component.config.maxSizeMB).toBe(10);
      expect(component.config.allowedTypes).toEqual(['application/pdf', 'image/jpeg', 'image/png']);
    });

    it('should initialize with existing document', () => {
      component.existingDocument = mockDocument;
      component.ngOnInit();
      
      component.documents$.subscribe(docs => {
        expect(docs.length).toBe(1);
        expect(docs[0]).toEqual(mockDocument);
      });
    });
  });

  describe('File Selection', () => {
    beforeEach(() => {
      component.ngOnInit();
      mockDocumentService.validateFileFormat.and.returnValue(true);
      mockDocumentService.validateFileSize.and.returnValue(true);
    });

    it('should handle file selection from input', () => {
      const mockFile = new File(['content'], 'test.pdf', { type: 'application/pdf' });
      const mockEvent = {
        target: { files: [mockFile] }
      } as any;

      component.onFileSelected(mockEvent);
      
      expect(component.selectedFile).toBe(mockFile);
      expect(mockDocumentService.validateFileFormat).toHaveBeenCalledWith(mockFile, mockConfig.allowedTypes);
      expect(mockDocumentService.validateFileSize).toHaveBeenCalledWith(mockFile, mockConfig.maxSizeMB);
    });

    it('should handle drag and drop file selection', () => {
      const mockFile = new File(['content'], 'test.pdf', { type: 'application/pdf' });
      const mockEvent = {
        preventDefault: jasmine.createSpy('preventDefault'),
        stopPropagation: jasmine.createSpy('stopPropagation'),
        dataTransfer: { files: [mockFile] }
      } as any;

      component.onDrop(mockEvent);
      
      expect(mockEvent.preventDefault).toHaveBeenCalled();
      expect(mockEvent.stopPropagation).toHaveBeenCalled();
      expect(component.selectedFile).toBe(mockFile);
      expect(component.dragOver).toBe(false);
    });

    it('should handle drag over and leave events', () => {
      const mockEvent = {
        preventDefault: jasmine.createSpy('preventDefault'),
        stopPropagation: jasmine.createSpy('stopPropagation')
      } as any;

      component.onDragOver(mockEvent);
      expect(component.dragOver).toBe(true);
      expect(mockEvent.preventDefault).toHaveBeenCalled();

      component.onDragLeave(mockEvent);
      expect(component.dragOver).toBe(false);
    });

    it('should generate preview for image files', () => {
      const mockImageFile = new File(['content'], 'test.jpg', { type: 'image/jpeg' });
      
      // Mock FileReader
      const mockFileReader = {
        onload: null as any,
        readAsDataURL: jasmine.createSpy('readAsDataURL').and.callFake(function(this: any) {
          if (this.onload) {
            this.onload({ target: { result: 'data:image/jpeg;base64,mockdata' } });
          }
        })
      };
      spyOn(window, 'FileReader').and.returnValue(mockFileReader as any);

      component['handleFileSelection'](mockImageFile);
      
      expect(mockFileReader.readAsDataURL).toHaveBeenCalledWith(mockImageFile);
      expect(component.previewUrl).toBe('data:image/jpeg;base64,mockdata');
    });

    it('should not generate preview for non-image files', () => {
      const mockPdfFile = new File(['content'], 'test.pdf', { type: 'application/pdf' });
      
      component['handleFileSelection'](mockPdfFile);
      
      expect(component.previewUrl).toBeNull();
    });
  });

  describe('File Validation', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should reject invalid file format', () => {
      const mockFile = new File(['content'], 'test.txt', { type: 'text/plain' });
      mockDocumentService.validateFileFormat.and.returnValue(false);
      mockDocumentService.validateFileSize.and.returnValue(true);

      component['handleFileSelection'](mockFile);
      
      expect(component.error).toContain('Format de fichier non autorisé');
      expect(component.selectedFile).toBeNull();
    });

    it('should reject oversized files', () => {
      const mockFile = new File(['content'], 'large.pdf', { type: 'application/pdf' });
      mockDocumentService.validateFileFormat.and.returnValue(true);
      mockDocumentService.validateFileSize.and.returnValue(false);

      component['handleFileSelection'](mockFile);
      
      expect(component.error).toContain('Fichier trop volumineux');
      expect(component.selectedFile).toBeNull();
    });

    it('should accept valid files', () => {
      const mockFile = new File(['content'], 'valid.pdf', { type: 'application/pdf' });
      mockDocumentService.validateFileFormat.and.returnValue(true);
      mockDocumentService.validateFileSize.and.returnValue(true);

      component['handleFileSelection'](mockFile);
      
      expect(component.error).toBeNull();
      expect(component.selectedFile).toBe(mockFile);
    });
  });

  describe('File Upload', () => {
    beforeEach(() => {
      component.ngOnInit();
      const mockFile = new File(['content'], 'test.pdf', { type: 'application/pdf' });
      component.selectedFile = mockFile;
    });

    it('should upload file successfully', () => {
      mockDocumentService.uploadDocument.and.returnValue(of(mockDocument));
      
      component.onUpload();
      
      expect(mockDocumentService.uploadDocument).toHaveBeenCalledWith(
        jasmine.objectContaining({
          file: jasmine.any(File),
          type: mockConfig.type,
          obligatoire: mockConfig.required
        }),
        component.inscriptionId
      );
      
      expect(component.success).toBe('Document uploadé avec succès');
      expect(component.uploading).toBe(false);
    });

    it('should handle upload error', () => {
      const error = { status: 413 }; // Don't include error.message to test the status-based logic
      mockDocumentService.uploadDocument.and.returnValue(throwError(() => error));
      
      component.onUpload();
      
      expect(component.error).toBe('Fichier trop volumineux');
      expect(component.uploading).toBe(false);
    });

    it('should not upload without selected file', () => {
      component.selectedFile = null;
      
      component.onUpload();
      
      expect(component.error).toBe('Aucun fichier sélectionné');
      expect(mockDocumentService.uploadDocument).not.toHaveBeenCalled();
    });

    it('should emit upload events', () => {
      spyOn(component.documentUploaded, 'emit');
      mockDocumentService.uploadDocument.and.returnValue(of(mockDocument));
      
      component.onUpload();
      
      expect(component.documentUploaded.emit).toHaveBeenCalledWith(mockDocument);
    });
  });

  describe('File Replacement', () => {
    beforeEach(() => {
      component.ngOnInit();
      component.existingDocument = mockDocument;
      const mockFile = new File(['content'], 'replacement.pdf', { type: 'application/pdf' });
      component.selectedFile = mockFile;
    });

    it('should replace existing document', () => {
      const updatedDocument = { ...mockDocument, nom: 'replacement.pdf' };
      mockDocumentService.replaceDocument.and.returnValue(of(updatedDocument));
      
      component.onReplace(mockDocument.id);
      
      expect(mockDocumentService.replaceDocument).toHaveBeenCalledWith(
        mockDocument.id,
        jasmine.any(File)
      );
      
      expect(component.success).toBe('Document remplacé avec succès');
    });

    it('should handle replacement error', () => {
      const error = { status: 500 }; // Don't include error.message to test the default case
      mockDocumentService.replaceDocument.and.returnValue(throwError(() => error));
      
      component.onReplace(mockDocument.id);
      
      expect(component.error).toBe('Erreur lors de l\'upload du document');
    });

    it('should not replace without selected file', () => {
      component.selectedFile = null;
      
      component.onReplace(mockDocument.id);
      
      expect(component.error).toBe('Aucun fichier sélectionné pour le remplacement');
      expect(mockDocumentService.replaceDocument).not.toHaveBeenCalled();
    });
  });

  describe('File Deletion', () => {
    beforeEach(() => {
      component.ngOnInit();
      component.existingDocument = mockDocument;
      // Mock confirm dialog
      spyOn(window, 'confirm').and.returnValue(true);
    });

    it('should delete document after confirmation', () => {
      spyOn(component.documentDeleted, 'emit');
      mockDocumentService.deleteDocument.and.returnValue(of(void 0));
      
      component.onDelete(mockDocument.id);
      
      expect(window.confirm).toHaveBeenCalledWith('Êtes-vous sûr de vouloir supprimer ce document ?');
      expect(mockDocumentService.deleteDocument).toHaveBeenCalledWith(mockDocument.id);
      expect(component.success).toBe('Document supprimé avec succès');
      expect(component.documentDeleted.emit).toHaveBeenCalledWith(mockDocument.id);
    });

    it('should not delete without confirmation', () => {
      (window.confirm as jasmine.Spy).and.returnValue(false);
      
      component.onDelete(mockDocument.id);
      
      expect(mockDocumentService.deleteDocument).not.toHaveBeenCalled();
    });

    it('should handle deletion error', () => {
      const error = { status: 500 };
      mockDocumentService.deleteDocument.and.returnValue(throwError(() => error));
      
      component.onDelete(mockDocument.id);
      
      expect(component.error).toBe('Erreur lors de la suppression du document');
    });
  });

  describe('File Download and Preview', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should download document', () => {
      const mockBlob = new Blob(['content'], { type: 'application/pdf' });
      mockDocumentService.downloadDocument.and.returnValue(of(mockBlob));
      
      // Mock URL and link creation
      spyOn(window.URL, 'createObjectURL').and.returnValue('blob:mock-url');
      spyOn(window.URL, 'revokeObjectURL');
      const mockLink = {
        href: '',
        download: '',
        click: jasmine.createSpy('click')
      };
      spyOn(document, 'createElement').and.returnValue(mockLink as any);
      
      component.onDownload(mockDocument.id, mockDocument.nom);
      
      expect(mockDocumentService.downloadDocument).toHaveBeenCalledWith(mockDocument.id);
      expect(mockLink.href).toBe('blob:mock-url');
      expect(mockLink.download).toBe(mockDocument.nom);
      expect(mockLink.click).toHaveBeenCalled();
      expect(window.URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-url');
    });

    it('should preview document', () => {
      const mockBlob = new Blob(['content'], { type: 'application/pdf' });
      mockDocumentService.downloadDocument.and.returnValue(of(mockBlob));
      
      spyOn(window.URL, 'createObjectURL').and.returnValue('blob:mock-url');
      spyOn(window, 'open');
      
      component.onPreview(mockDocument.id);
      
      expect(mockDocumentService.downloadDocument).toHaveBeenCalledWith(mockDocument.id);
      expect(window.open).toHaveBeenCalledWith('blob:mock-url', '_blank');
    });

    it('should handle download error', () => {
      const error = { status: 404 };
      mockDocumentService.downloadDocument.and.returnValue(throwError(() => error));
      
      component.onDownload(mockDocument.id, mockDocument.nom);
      
      expect(component.error).toBe('Erreur lors du téléchargement du document');
    });
  });

  describe('Helper Methods and Properties', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should detect existing document correctly', () => {
      expect(component.hasExistingDocument).toBe(false);
      
      component.existingDocument = mockDocument;
      component.ngOnInit();
      
      expect(component.hasExistingDocument).toBe(true);
    });

    it('should return current document', () => {
      expect(component.currentDocument).toBeNull();
      
      component.existingDocument = mockDocument;
      component.ngOnInit();
      
      expect(component.currentDocument).toEqual(mockDocument);
    });

    it('should determine upload capability', () => {
      expect(component.canUpload).toBe(false);
      
      component.selectedFile = new File(['content'], 'test.pdf', { type: 'application/pdf' });
      expect(component.canUpload).toBe(true);
      
      component.disabled = true;
      expect(component.canUpload).toBe(false);
    });

    it('should determine replacement capability', () => {
      component.selectedFile = new File(['content'], 'test.pdf', { type: 'application/pdf' });
      expect(component.canReplace).toBe(false);
      
      component.existingDocument = mockDocument;
      component.ngOnInit();
      expect(component.canReplace).toBe(true);
    });

    it('should show upload area appropriately', () => {
      expect(component.showUploadArea).toBe(true);
      
      component.existingDocument = mockDocument;
      component.ngOnInit();
      expect(component.showUploadArea).toBe(false);
      
      component.selectedFile = new File(['content'], 'test.pdf', { type: 'application/pdf' });
      expect(component.showUploadArea).toBe(true);
    });

    it('should format file size using service', () => {
      mockDocumentService.formatFileSize.and.returnValue('1.0 MB');
      
      const result = component.formatFileSize(1024000);
      
      expect(mockDocumentService.formatFileSize).toHaveBeenCalledWith(1024000);
      expect(result).toBe('1.0 MB');
    });

    it('should get document icon using service', () => {
      mockDocumentService.getDocumentTypeIcon.and.returnValue('fa-file-pdf');
      
      const result = component.getDocumentIcon();
      
      expect(mockDocumentService.getDocumentTypeIcon).toHaveBeenCalledWith(mockConfig.type);
      expect(result).toBe('fa-file-pdf');
    });
  });

  describe('Error Message Handling', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should return appropriate error messages for different HTTP codes', () => {
      const testCases = [
        { status: 413, expected: 'Fichier trop volumineux' },
        { status: 415, expected: 'Format de fichier non supporté' },
        { status: 500, expected: 'Erreur lors de l\'upload du document' }
      ];

      testCases.forEach(testCase => {
        const error = { status: testCase.status };
        const message = component['getUploadErrorMessage'](error);
        expect(message).toBe(testCase.expected);
      });
    });

    it('should use custom error message when available', () => {
      const error = { error: { message: 'Custom error' } };
      const message = component['getUploadErrorMessage'](error);
      expect(message).toBe('Custom error');
    });
  });
});