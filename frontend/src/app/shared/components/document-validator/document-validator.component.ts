import { Component, Input, Output, EventEmitter, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { BehaviorSubject, Subject, Observable } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { DocumentService } from '../../../core/services/document.service';
import { DocumentResponse, DocumentType } from '../../../core/models/document.model';

export interface ValidationResult {
  isValid: boolean;
  errors: ValidationError[];
  warnings: ValidationWarning[];
  fileInfo: FileInfo;
}

export interface ValidationError {
  code: string;
  message: string;
  severity: 'error' | 'warning';
}

export interface ValidationWarning {
  code: string;
  message: string;
  recommendation?: string;
}

export interface FileInfo {
  name: string;
  size: number;
  type: string;
  lastModified: Date;
  checksum?: string;
}

export interface ValidationConfig {
  allowedTypes: string[];
  maxSizeMB: number;
  minSizeMB?: number;
  requireSignature?: boolean;
  allowedExtensions?: string[];
  checkIntegrity?: boolean;
}

@Component({
  selector: 'app-document-validator',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatProgressSpinnerModule, MatButtonModule],
  templateUrl: './document-validator.component.html',
  styleUrls: ['./document-validator.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DocumentValidatorComponent implements OnInit {
  @Input() file: File | null = null;
  @Input() document: DocumentResponse | null = null;
  @Input() documentType: DocumentType | null = null;
  @Input() config: ValidationConfig | null = null;
  @Input() autoValidate = true;
  
  @Output() validationComplete = new EventEmitter<ValidationResult>();
  @Output() validationError = new EventEmitter<string>();

  private destroy$ = new Subject<void>();
  private validatingSubject = new BehaviorSubject<boolean>(false);
  private resultSubject = new BehaviorSubject<ValidationResult | null>(null);
  
  validating$ = this.validatingSubject.asObservable();
  result$ = this.resultSubject.asObservable();

  // Default validation configurations by document type
  private defaultConfigs: { [key in DocumentType]: ValidationConfig } = {
    [DocumentType.CARTE_IDENTITE]: {
      allowedTypes: ['application/pdf', 'image/jpeg', 'image/png'],
      maxSizeMB: 5,
      minSizeMB: 0.01,
      allowedExtensions: ['.pdf', '.jpg', '.jpeg', '.png'],
      checkIntegrity: true
    },
    [DocumentType.DIPLOME_MASTER]: {
      allowedTypes: ['application/pdf'],
      maxSizeMB: 10,
      minSizeMB: 0.1,
      allowedExtensions: ['.pdf'],
      checkIntegrity: true,
      requireSignature: false
    },
    [DocumentType.RELEVES_NOTES]: {
      allowedTypes: ['application/pdf'],
      maxSizeMB: 10,
      minSizeMB: 0.1,
      allowedExtensions: ['.pdf'],
      checkIntegrity: true
    },
    [DocumentType.CV]: {
      allowedTypes: ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'],
      maxSizeMB: 5,
      minSizeMB: 0.01,
      allowedExtensions: ['.pdf', '.doc', '.docx'],
      checkIntegrity: true
    },
    [DocumentType.LETTRE_MOTIVATION]: {
      allowedTypes: ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'],
      maxSizeMB: 5,
      minSizeMB: 0.01,
      allowedExtensions: ['.pdf', '.doc', '.docx'],
      checkIntegrity: true
    },
    [DocumentType.MANUSCRIT_THESE]: {
      allowedTypes: ['application/pdf'],
      maxSizeMB: 50,
      minSizeMB: 1,
      allowedExtensions: ['.pdf'],
      checkIntegrity: true,
      requireSignature: false
    },
    [DocumentType.RESUME_THESE]: {
      allowedTypes: ['application/pdf'],
      maxSizeMB: 5,
      minSizeMB: 0.1,
      allowedExtensions: ['.pdf'],
      checkIntegrity: true
    },
    [DocumentType.PUBLICATIONS]: {
      allowedTypes: ['application/pdf'],
      maxSizeMB: 20,
      minSizeMB: 0.1,
      allowedExtensions: ['.pdf'],
      checkIntegrity: true
    },
    [DocumentType.ATTESTATION_FORMATION]: {
      allowedTypes: ['application/pdf'],
      maxSizeMB: 10,
      minSizeMB: 0.1,
      allowedExtensions: ['.pdf'],
      checkIntegrity: true
    },
    [DocumentType.AUTORISATION_SOUTENANCE]: {
      allowedTypes: ['application/pdf'],
      maxSizeMB: 10,
      minSizeMB: 0.1,
      allowedExtensions: ['.pdf'],
      checkIntegrity: true,
      requireSignature: true
    }
  };

  constructor(private documentService: DocumentService) {}

  ngOnInit(): void {
    if (this.autoValidate && (this.file || this.document)) {
      this.validateDocument();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  validateDocument(): void {
    if (!this.file && !this.document) {
      this.validationError.emit('Aucun fichier ou document à valider');
      return;
    }

    this.validatingSubject.next(true);
    
    try {
      const result = this.performValidation();
      this.resultSubject.next(result);
      this.validationComplete.emit(result);
    } catch (error) {
      console.error('Erreur validation document:', error);
      this.validationError.emit('Erreur lors de la validation du document');
    } finally {
      this.validatingSubject.next(false);
    }
  }

  private performValidation(): ValidationResult {
    const config = this.getValidationConfig();
    const fileInfo = this.getFileInfo();
    const errors: ValidationError[] = [];
    const warnings: ValidationWarning[] = [];

    // Validate file type
    if (!this.validateFileType(fileInfo, config)) {
      errors.push({
        code: 'INVALID_FILE_TYPE',
        message: `Type de fichier non autorisé. Types acceptés: ${this.getFormattedAllowedTypes(config)}`,
        severity: 'error'
      });
    }

    // Validate file size
    const sizeValidation = this.validateFileSize(fileInfo, config);
    if (!sizeValidation.isValid) {
      errors.push({
        code: sizeValidation.code,
        message: sizeValidation.message,
        severity: 'error'
      });
    }

    // Validate file extension
    if (!this.validateFileExtension(fileInfo, config)) {
      errors.push({
        code: 'INVALID_FILE_EXTENSION',
        message: `Extension de fichier non autorisée. Extensions acceptées: ${config.allowedExtensions?.join(', ')}`,
        severity: 'error'
      });
    }

    // Check for potential issues
    const potentialIssues = this.checkPotentialIssues(fileInfo, config);
    warnings.push(...potentialIssues);

    // Validate file integrity (basic check)
    if (config.checkIntegrity && !this.validateFileIntegrity(fileInfo)) {
      warnings.push({
        code: 'INTEGRITY_WARNING',
        message: 'Le fichier pourrait être corrompu ou incomplet',
        recommendation: 'Vérifiez que le fichier s\'ouvre correctement'
      });
    }

    // Check for signature requirement
    if (config.requireSignature && !this.hasDigitalSignature(fileInfo)) {
      warnings.push({
        code: 'MISSING_SIGNATURE',
        message: 'Ce type de document devrait être signé électroniquement',
        recommendation: 'Ajoutez une signature électronique si possible'
      });
    }

    return {
      isValid: errors.length === 0,
      errors,
      warnings,
      fileInfo
    };
  }

  private getValidationConfig(): ValidationConfig {
    if (this.config) {
      return this.config;
    }

    if (this.documentType && this.defaultConfigs[this.documentType]) {
      return this.defaultConfigs[this.documentType];
    }

    // Default fallback config
    return {
      allowedTypes: ['application/pdf', 'image/jpeg', 'image/png'],
      maxSizeMB: 10,
      minSizeMB: 0.01,
      allowedExtensions: ['.pdf', '.jpg', '.jpeg', '.png'],
      checkIntegrity: true
    };
  }

  private getFileInfo(): FileInfo {
    if (this.file) {
      return {
        name: this.file.name,
        size: this.file.size,
        type: this.file.type,
        lastModified: new Date(this.file.lastModified)
      };
    }

    if (this.document) {
      return {
        name: this.document.nom,
        size: this.document.taille,
        type: this.getDocumentMimeType(this.document.nom),
        lastModified: new Date(this.document.dateUpload)
      };
    }

    throw new Error('Aucun fichier ou document disponible');
  }

  private getDocumentMimeType(fileName: string): string {
    const extension = fileName.toLowerCase().split('.').pop();
    const mimeTypes: { [key: string]: string } = {
      'pdf': 'application/pdf',
      'jpg': 'image/jpeg',
      'jpeg': 'image/jpeg',
      'png': 'image/png',
      'doc': 'application/msword',
      'docx': 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    };
    return mimeTypes[extension || ''] || 'application/octet-stream';
  }

  private validateFileType(fileInfo: FileInfo, config: ValidationConfig): boolean {
    return config.allowedTypes.includes(fileInfo.type);
  }

  private validateFileSize(fileInfo: FileInfo, config: ValidationConfig): { isValid: boolean; code: string; message: string } {
    const sizeMB = fileInfo.size / (1024 * 1024);
    
    if (sizeMB > config.maxSizeMB) {
      return {
        isValid: false,
        code: 'FILE_TOO_LARGE',
        message: `Fichier trop volumineux (${sizeMB.toFixed(2)} MB). Taille maximale: ${config.maxSizeMB} MB`
      };
    }

    if (config.minSizeMB && sizeMB < config.minSizeMB) {
      return {
        isValid: false,
        code: 'FILE_TOO_SMALL',
        message: `Fichier trop petit (${sizeMB.toFixed(2)} MB). Taille minimale: ${config.minSizeMB} MB`
      };
    }

    return { isValid: true, code: '', message: '' };
  }

  private validateFileExtension(fileInfo: FileInfo, config: ValidationConfig): boolean {
    if (!config.allowedExtensions) return true;
    
    const extension = '.' + fileInfo.name.toLowerCase().split('.').pop();
    return config.allowedExtensions.includes(extension);
  }

  private validateFileIntegrity(fileInfo: FileInfo): boolean {
    // Basic integrity check - in a real implementation, this would be more sophisticated
    // For now, just check if file size is reasonable and name is not suspicious
    return fileInfo.size > 0 && 
           fileInfo.name.length > 0 && 
           !fileInfo.name.includes('..') &&
           fileInfo.size < 100 * 1024 * 1024; // Max 100MB as sanity check
  }

  private hasDigitalSignature(fileInfo: FileInfo): boolean {
    // This is a placeholder - real signature validation would require PDF parsing
    // For now, we'll assume PDFs might have signatures
    return fileInfo.type === 'application/pdf';
  }

  private checkPotentialIssues(fileInfo: FileInfo, config: ValidationConfig): ValidationWarning[] {
    const warnings: ValidationWarning[] = [];
    const sizeMB = fileInfo.size / (1024 * 1024);

    // Check if file is very large (but still within limits)
    if (sizeMB > config.maxSizeMB * 0.8) {
      warnings.push({
        code: 'LARGE_FILE_WARNING',
        message: 'Fichier volumineux détecté',
        recommendation: 'Considérez compresser le fichier si possible'
      });
    }

    // Check file age
    const daysSinceModified = (Date.now() - fileInfo.lastModified.getTime()) / (1000 * 60 * 60 * 24);
    if (daysSinceModified > 365) {
      warnings.push({
        code: 'OLD_FILE_WARNING',
        message: 'Fichier ancien détecté (plus d\'un an)',
        recommendation: 'Vérifiez que le document est toujours valide'
      });
    }

    // Check for suspicious file names
    if (this.hasSuspiciousFileName(fileInfo.name)) {
      warnings.push({
        code: 'SUSPICIOUS_FILENAME',
        message: 'Nom de fichier inhabituel détecté',
        recommendation: 'Utilisez un nom de fichier descriptif et professionnel'
      });
    }

    return warnings;
  }

  private hasSuspiciousFileName(fileName: string): boolean {
    const suspiciousPatterns = [
      /temp/i,
      /test/i,
      /copy/i,
      /untitled/i,
      /document\d+/i,
      /^[a-f0-9]{8,}$/i // Looks like a hash
    ];

    return suspiciousPatterns.some(pattern => pattern.test(fileName));
  }

  private getFormattedAllowedTypes(config: ValidationConfig): string {
    const typeLabels: { [key: string]: string } = {
      'application/pdf': 'PDF',
      'image/jpeg': 'JPEG',
      'image/png': 'PNG',
      'application/msword': 'Word (DOC)',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'Word (DOCX)'
    };

    return config.allowedTypes
      .map(type => typeLabels[type] || type)
      .join(', ');
  }

  // Helper methods for template
  get hasResult(): boolean {
    return this.resultSubject.value !== null;
  }

  get isValid(): boolean {
    const result = this.resultSubject.value;
    return result ? result.isValid : false;
  }

  get hasErrors(): boolean {
    const result = this.resultSubject.value;
    return result ? result.errors.length > 0 : false;
  }

  get hasWarnings(): boolean {
    const result = this.resultSubject.value;
    return result ? result.warnings.length > 0 : false;
  }

  get errorCount(): number {
    const result = this.resultSubject.value;
    return result ? result.errors.length : 0;
  }

  get warningCount(): number {
    const result = this.resultSubject.value;
    return result ? result.warnings.length : 0;
  }

  formatFileSize(bytes: number): string {
    return this.documentService.formatFileSize(bytes);
  }

  onRetry(): void {
    this.validateDocument();
  }
}