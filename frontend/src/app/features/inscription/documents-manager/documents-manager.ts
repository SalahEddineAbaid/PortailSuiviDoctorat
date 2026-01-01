import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs';

import { DocumentUpload, DocumentUploadConfig } from '../document-upload/document-upload';
import { DocumentService } from '../../../core/services/document.service';
import { TypeDocument, DocumentResponse } from '../../../core/models/document.model';

@Component({
  selector: 'app-documents-manager',
  standalone: true,
  imports: [CommonModule, DocumentUpload],
  templateUrl: './documents-manager.html',
  styleUrls: ['./documents-manager.scss']
})
export class DocumentsManager implements OnInit {
  @Input() inscriptionId?: number;
  @Input() disabled = false;
  @Input() showTitle = true;

  documents$!: Observable<DocumentResponse[]>;
  loading = false;
  error: string | null = null;

  // Configuration des documents requis pour une inscription
  documentConfigs: DocumentUploadConfig[] = [
    {
      type: TypeDocument.DIPLOME_MASTER,
      label: 'Diplôme de Master',
      required: true,
      maxSizeMB: 10,
      allowedTypes: ['application/pdf', 'image/jpeg', 'image/png'],
      description: 'Copie de votre diplôme de Master ou équivalent'
    },
    {
      type: TypeDocument.RELEVE_NOTES,
      label: 'Relevés de notes',
      required: true,
      maxSizeMB: 10,
      allowedTypes: ['application/pdf'],
      description: 'Relevés de notes du Master (toutes les années)'
    },
    {
      type: TypeDocument.CV,
      label: 'Curriculum Vitae',
      required: true,
      maxSizeMB: 5,
      allowedTypes: ['application/pdf'],
      description: 'CV détaillé incluant votre parcours académique et professionnel'
    },
    {
      type: TypeDocument.LETTRE_MOTIVATION,
      label: 'Lettre de motivation',
      required: true,
      maxSizeMB: 5,
      allowedTypes: ['application/pdf'],
      description: 'Lettre de motivation pour votre projet de thèse'
    },
    {
      type: TypeDocument.PROJET_THESE,
      label: 'Projet de thèse',
      required: true,
      maxSizeMB: 10,
      allowedTypes: ['application/pdf'],
      description: 'Description détaillée de votre projet de recherche'
    }
  ];

  existingDocuments: { [key: string]: DocumentResponse } = {};

  constructor(private documentService: DocumentService) {}

  ngOnInit(): void {
    if (this.inscriptionId) {
      this.loadDocuments();
    }
  }

  loadDocuments(): void {
    if (!this.inscriptionId) return;

    this.loading = true;
    this.error = null;

    this.documents$ = this.documentService.getDocuments(this.inscriptionId);
    
    this.documents$.subscribe({
      next: (documents) => {
        this.existingDocuments = {};
        documents.forEach(doc => {
          this.existingDocuments[doc.typeDocument] = doc;
        });
        this.loading = false;
      },
      error: (error) => {
        this.error = 'Erreur lors du chargement des documents';
        this.loading = false;
        console.error('Erreur chargement documents:', error);
      }
    });
  }

  onDocumentUploaded(document: DocumentResponse): void {
    this.existingDocuments[document.typeDocument] = document;
    // Optionally emit event to parent component
  }

  onDocumentDeleted(documentId: number): void {
    // Find and remove the document from existingDocuments
    Object.keys(this.existingDocuments).forEach(type => {
      if (this.existingDocuments[type].id === documentId) {
        delete this.existingDocuments[type];
      }
    });
  }

  onUploadError(error: string): void {
    this.error = error;
  }

  getExistingDocument(type: TypeDocument): DocumentResponse | undefined {
    return this.existingDocuments[type];
  }

  getCompletionStatus(): { completed: number; total: number; percentage: number } {
    const requiredConfigs = this.documentConfigs.filter(config => config.required);
    const completedRequired = requiredConfigs.filter(config => 
      this.existingDocuments[config.type]
    ).length;

    return {
      completed: completedRequired,
      total: requiredConfigs.length,
      percentage: Math.round((completedRequired / requiredConfigs.length) * 100)
    };
  }

  isAllRequiredDocumentsUploaded(): boolean {
    const status = this.getCompletionStatus();
    return status.completed === status.total;
  }

  getMissingRequiredDocuments(): DocumentUploadConfig[] {
    return this.documentConfigs.filter(config => 
      config.required && !this.existingDocuments[config.type]
    );
  }
}