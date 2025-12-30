import { Component, Input, Output, EventEmitter, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BehaviorSubject } from 'rxjs';

import { DocumentService } from '../../../core/services/document.service';
import { 
  DocumentType, 
  DocumentResponse, 
  DocumentUploadRequest 
} from '../../../core/models/document.model';

export interface DocumentUploadConfig {
  type: DocumentType;
  label: string;
  required: boolean;
  maxSizeMB?: number;
  allowedTypes?: string[];
  description?: string;
}

@Component({
  selector: 'app-document-upload',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './document-upload.html',
  styleUrls: ['./document-upload.scss']
})
export class DocumentUpload implements OnInit {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;
  
  @Input() config!: DocumentUploadConfig;
  @Input() inscriptionId?: number;
  @Input() existingDocument?: DocumentResponse;
  @Input() disabled = false;
  
  @Output() documentUploaded = new EventEmitter<DocumentResponse>();
  @Output() documentDeleted = new EventEmitter<number>();
  @Output() uploadError = new EventEmitter<string>();

  private documentsSubject = new BehaviorSubject<DocumentResponse[]>([]);
  documents$ = this.documentsSubject.asObservable();

  uploading = false;
  uploadProgress = 0;
  error: string | null = null;
  success: string | null = null;
  
  selectedFile: File | null = null;
  previewUrl: string | null = null;
  dragOver = false;

  // Default configuration
  private defaultConfig = {
    maxSizeMB: 10,
    allowedTypes: ['application/pdf', 'image/jpeg', 'image/png']
  };

  constructor(private documentService: DocumentService) {}

  ngOnInit(): void {
    this.validateConfig();
    if (this.existingDocument) {
      this.documentsSubject.next([this.existingDocument]);
    }
  }

  private validateConfig(): void {
    if (!this.config) {
      throw new Error('DocumentUpload: config is required');
    }
    
    // Apply defaults
    this.config.maxSizeMB = this.config.maxSizeMB || this.defaultConfig.maxSizeMB;
    this.config.allowedTypes = this.config.allowedTypes || this.defaultConfig.allowedTypes;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFileSelection(input.files[0]);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = false;

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.handleFileSelection(files[0]);
    }
  }

  private handleFileSelection(file: File): void {
    this.clearMessages();
    
    if (!this.validateFile(file)) {
      return;
    }

    this.selectedFile = file;
    this.generatePreview(file);
  }

  private validateFile(file: File): boolean {
    // Check file type
    if (!this.documentService.validateFileFormat(file, this.config.allowedTypes)) {
      this.error = `Format de fichier non autorisé. Formats acceptés: ${this.getFormatsLabel()}`;
      return false;
    }

    // Check file size
    if (!this.documentService.validateFileSize(file, this.config.maxSizeMB!)) {
      this.error = `Fichier trop volumineux. Taille maximale: ${this.config.maxSizeMB} MB`;
      return false;
    }

    return true;
  }

  private generatePreview(file: File): void {
    if (file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = (e) => {
        this.previewUrl = e.target?.result as string;
      };
      reader.readAsDataURL(file);
    } else {
      this.previewUrl = null;
    }
  }

  onUpload(): void {
    if (!this.selectedFile) {
      this.error = 'Aucun fichier sélectionné';
      return;
    }

    this.uploading = true;
    this.clearMessages();

    const uploadRequest: DocumentUploadRequest = {
      file: this.selectedFile,
      type: this.config.type,
      obligatoire: this.config.required
    };

    this.documentService.uploadDocument(uploadRequest, this.inscriptionId).subscribe({
      next: (response) => {
        this.success = 'Document uploadé avec succès';
        this.documentsSubject.next([response]);
        this.documentUploaded.emit(response);
        this.resetUpload();
        this.uploading = false;
      },
      error: (error) => {
        this.error = this.getUploadErrorMessage(error);
        this.uploadError.emit(this.error);
        this.uploading = false;
        console.error('Erreur upload document:', error);
      }
    });
  }

  onReplace(documentId: number): void {
    if (!this.selectedFile) {
      this.error = 'Aucun fichier sélectionné pour le remplacement';
      return;
    }

    this.uploading = true;
    this.clearMessages();

    this.documentService.replaceDocument(documentId, this.selectedFile).subscribe({
      next: (response) => {
        this.success = 'Document remplacé avec succès';
        this.documentsSubject.next([response]);
        this.documentUploaded.emit(response);
        this.resetUpload();
        this.uploading = false;
      },
      error: (error) => {
        this.error = this.getUploadErrorMessage(error);
        this.uploadError.emit(this.error);
        this.uploading = false;
        console.error('Erreur remplacement document:', error);
      }
    });
  }

  onDelete(documentId: number): void {
    if (!confirm('Êtes-vous sûr de vouloir supprimer ce document ?')) {
      return;
    }

    this.documentService.deleteDocument(documentId).subscribe({
      next: () => {
        this.success = 'Document supprimé avec succès';
        this.documentsSubject.next([]);
        this.documentDeleted.emit(documentId);
      },
      error: (error) => {
        this.error = 'Erreur lors de la suppression du document';
        console.error('Erreur suppression document:', error);
      }
    });
  }

  onDownload(documentId: number, fileName: string): void {
    this.documentService.downloadDocument(documentId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        this.error = 'Erreur lors du téléchargement du document';
        console.error('Erreur téléchargement document:', error);
      }
    });
  }

  onPreview(documentId: number): void {
    this.documentService.downloadDocument(documentId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
      },
      error: (error) => {
        this.error = 'Erreur lors de l\'ouverture du document';
        console.error('Erreur prévisualisation document:', error);
      }
    });
  }

  resetUpload(): void {
    this.selectedFile = null;
    this.previewUrl = null;
    this.uploadProgress = 0;
    
    // Reset file input
    if (this.fileInput?.nativeElement) {
      this.fileInput.nativeElement.value = '';
    }
  }

  triggerFileInput(): void {
    if (this.fileInput?.nativeElement) {
      this.fileInput.nativeElement.click();
    }
  }

  private clearMessages(): void {
    this.error = null;
    this.success = null;
  }

  private getUploadErrorMessage(error: any): string {
    if (error.error?.message) {
      return error.error.message;
    }
    if (error.status === 413) {
      return 'Fichier trop volumineux';
    }
    if (error.status === 415) {
      return 'Format de fichier non supporté';
    }
    return 'Erreur lors de l\'upload du document';
  }

  // Helper methods for template
  get hasExistingDocument(): boolean {
    return this.documentsSubject.value.length > 0;
  }

  get currentDocument(): DocumentResponse | null {
    const docs = this.documentsSubject.value;
    return docs.length > 0 ? docs[0] : null;
  }

  get canUpload(): boolean {
    return !this.disabled && !this.uploading && !!this.selectedFile;
  }

  get canReplace(): boolean {
    return !this.disabled && !this.uploading && !!this.selectedFile && this.hasExistingDocument;
  }

  get showUploadArea(): boolean {
    return !this.hasExistingDocument || !!this.selectedFile;
  }

  formatFileSize(bytes: number): string {
    return this.documentService.formatFileSize(bytes);
  }

  getDocumentIcon(): string {
    return this.documentService.getDocumentTypeIcon(this.config.type);
  }

  getFormatsLabel(): string {
    const formatLabels: { [key: string]: string } = {
      'application/pdf': 'PDF',
      'image/jpeg': 'JPEG',
      'image/png': 'PNG'
    };
    
    return this.config.allowedTypes!
      .map(type => formatLabels[type] || type)
      .join(', ');
  }
}