import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpEventType, HttpRequest } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  DocumentResponse,
  DocumentUploadRequest,
  TypeDocument,
  validateFile,
  formatFileSize,
  getMimeTypeIcon,
  ALLOWED_FILE_TYPES,
  MAX_FILE_SIZE_MB
} from '../models/document.model';

export interface UploadProgress {
  progress: number;
  status: 'uploading' | 'complete' | 'error';
}

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private readonly apiUrl = `${environment.apiUrl}/documents`;

  constructor(private http: HttpClient) {}

  // ============================================
  // Upload Operations
  // ============================================

  /**
   * Upload un document avec suivi de progression
   */
  uploadDocument(
    inscriptionId: number,
    file: File,
    typeDocument: TypeDocument
  ): Observable<HttpEvent<DocumentResponse>> {
    // Validation du fichier
    const validationError = validateFile(file);
    if (validationError) {
      return throwError(() => new Error(validationError.message));
    }

    const formData = new FormData();
    formData.append('file', file);
    formData.append('typeDocument', typeDocument);

    const req = new HttpRequest<DocumentResponse>(
      'POST',
      `${this.apiUrl}/${inscriptionId}/upload`,
      formData,
      {
        reportProgress: true
      }
    );

    return this.http.request<DocumentResponse>(req).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Upload un document avec progression simplifiée
   */
  uploadDocumentSimple(
    inscriptionId: number,
    file: File,
    typeDocument: TypeDocument
  ): Observable<DocumentResponse> {
    return this.uploadDocument(inscriptionId, file, typeDocument).pipe(
      map(event => {
        if (event.type === HttpEventType.Response) {
          return event.body!;
        }
        throw new Error('Upload en cours');
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Calculer la progression de l'upload
   */
  getUploadProgress(event: HttpEvent<any>): UploadProgress {
    switch (event.type) {
      case HttpEventType.UploadProgress:
        const progress = event.total
          ? Math.round((100 * event.loaded) / event.total)
          : 0;
        return { progress, status: 'uploading' };

      case HttpEventType.Response:
        return { progress: 100, status: 'complete' };

      default:
        return { progress: 0, status: 'uploading' };
    }
  }

  // ============================================
  // CRUD Operations
  // ============================================

  /**
   * Récupérer les documents d'une inscription
   */
  getDocuments(inscriptionId: number): Observable<DocumentResponse[]> {
    return this.http.get<DocumentResponse[]>(`${this.apiUrl}/${inscriptionId}`).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Télécharger un document
   */
  downloadDocument(documentId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/download/${documentId}`, {
      responseType: 'blob'
    }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Supprimer un document
   */
  deleteDocument(documentId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${documentId}`).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Remplacer un document existant
   */
  replaceDocument(documentId: number, file: File): Observable<DocumentResponse> {
    // Validation du fichier
    const validationError = validateFile(file);
    if (validationError) {
      return throwError(() => new Error(validationError.message));
    }

    const formData = new FormData();
    formData.append('file', file);

    return this.http.put<DocumentResponse>(`${this.apiUrl}/${documentId}`, formData).pipe(
      catchError(this.handleError)
    );
  }

  // ============================================
  // Validation Methods
  // ============================================

  /**
   * Valider le format du fichier
   */
  validateFileFormat(file: File, allowedTypes: string[] = ALLOWED_FILE_TYPES): boolean {
    return allowedTypes.includes(file.type);
  }

  /**
   * Valider la taille du fichier
   */
  validateFileSize(file: File, maxSizeMB: number = MAX_FILE_SIZE_MB): boolean {
    const maxSizeBytes = maxSizeMB * 1024 * 1024;
    return file.size <= maxSizeBytes;
  }

  /**
   * Obtenir le message d'erreur de validation
   */
  getValidationErrorMessage(file: File): string | null {
    if (!this.validateFileFormat(file)) {
      return `Format de fichier non autorisé. Formats acceptés: PDF, JPEG, PNG`;
    }

    if (!this.validateFileSize(file)) {
      return `Fichier trop volumineux. Taille maximale: ${MAX_FILE_SIZE_MB} MB`;
    }

    return null;
  }

  // ============================================
  // Helper Methods
  // ============================================

  /**
   * Formater la taille du fichier
   */
  formatFileSize(bytes: number): string {
    return formatFileSize(bytes);
  }

  /**
   * Obtenir l'icône du type de document
   */
  getDocumentTypeIcon(type: TypeDocument): string {
    const icons: { [key in TypeDocument]: string } = {
      [TypeDocument.DIPLOME_MASTER]: 'school',
      [TypeDocument.CV]: 'description',
      [TypeDocument.LETTRE_MOTIVATION]: 'mail',
      [TypeDocument.RELEVE_NOTES]: 'assessment',
      [TypeDocument.PROJET_THESE]: 'article',
      [TypeDocument.AUTORISATION_DIRECTEUR]: 'verified',
      [TypeDocument.AUTRE]: 'attach_file'
    };
    return icons[type];
  }

  /**
   * Obtenir l'icône du type MIME
   */
  getMimeTypeIcon(mimeType: string): string {
    return getMimeTypeIcon(mimeType);
  }

  /**
   * Vérifier si le document peut être prévisualisé
   */
  canPreview(mimeType: string): boolean {
    return mimeType === 'application/pdf' || mimeType.startsWith('image/');
  }

  /**
   * Télécharger et sauvegarder un document
   */
  downloadAndSave(documentId: number, fileName: string): void {
    this.downloadDocument(documentId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        console.error('Error downloading document:', error);
      }
    });
  }

  /**
   * Ouvrir un document dans un nouvel onglet
   */
  openInNewTab(documentId: number): void {
    this.downloadDocument(documentId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
      },
      error: (error) => {
        console.error('Error opening document:', error);
      }
    });
  }

  /**
   * Créer une URL de prévisualisation pour un fichier
   */
  createPreviewUrl(file: File): Observable<string> {
    return new Observable(observer => {
      const reader = new FileReader();
      
      reader.onload = (e) => {
        observer.next(e.target?.result as string);
        observer.complete();
      };
      
      reader.onerror = (error) => {
        observer.error(error);
      };
      
      reader.readAsDataURL(file);
    });
  }

  /**
   * Vérifier si tous les documents requis sont uploadés
   */
  areAllRequiredDocumentsUploaded(documents: DocumentResponse[]): boolean {
    const requiredTypes = [
      TypeDocument.DIPLOME_MASTER,
      TypeDocument.CV,
      TypeDocument.LETTRE_MOTIVATION,
      TypeDocument.RELEVE_NOTES,
      TypeDocument.PROJET_THESE,
      TypeDocument.AUTORISATION_DIRECTEUR
    ];

    const uploadedTypes = documents.map(doc => doc.typeDocument);
    return requiredTypes.every(type => uploadedTypes.includes(type));
  }

  /**
   * Obtenir les documents manquants
   */
  getMissingDocuments(documents: DocumentResponse[]): TypeDocument[] {
    const requiredTypes = [
      TypeDocument.DIPLOME_MASTER,
      TypeDocument.CV,
      TypeDocument.LETTRE_MOTIVATION,
      TypeDocument.RELEVE_NOTES,
      TypeDocument.PROJET_THESE,
      TypeDocument.AUTORISATION_DIRECTEUR
    ];

    const uploadedTypes = documents.map(doc => doc.typeDocument);
    return requiredTypes.filter(type => !uploadedTypes.includes(type));
  }

  /**
   * Calculer le pourcentage de complétion des documents
   */
  getDocumentsCompletionPercentage(documents: DocumentResponse[]): number {
    const requiredCount = 6; // Nombre de documents requis
    const uploadedCount = Math.min(documents.length, requiredCount);
    return Math.round((uploadedCount / requiredCount) * 100);
  }

  /**
   * Grouper les documents par type
   */
  groupDocumentsByType(documents: DocumentResponse[]): Map<TypeDocument, DocumentResponse[]> {
    const grouped = new Map<TypeDocument, DocumentResponse[]>();
    
    documents.forEach(doc => {
      const existing = grouped.get(doc.typeDocument) || [];
      grouped.set(doc.typeDocument, [...existing, doc]);
    });
    
    return grouped;
  }

  /**
   * Obtenir le document le plus récent d'un type
   */
  getLatestDocumentOfType(
    documents: DocumentResponse[],
    type: TypeDocument
  ): DocumentResponse | null {
    const filtered = documents.filter(doc => doc.typeDocument === type);
    
    if (filtered.length === 0) {
      return null;
    }
    
    return filtered.reduce((latest, current) => {
      const latestDate = new Date(latest.dateUpload);
      const currentDate = new Date(current.dateUpload);
      return currentDate > latestDate ? current : latest;
    });
  }

  // ============================================
  // Batch Operations
  // ============================================

  /**
   * Upload multiple documents
   */
  uploadMultipleDocuments(
    inscriptionId: number,
    files: Array<{ file: File; type: TypeDocument }>
  ): Observable<DocumentResponse[]> {
    const uploads = files.map(({ file, type }) =>
      this.uploadDocumentSimple(inscriptionId, file, type)
    );

    return new Observable(observer => {
      const results: DocumentResponse[] = [];
      let completed = 0;

      uploads.forEach(upload => {
        upload.subscribe({
          next: (response) => {
            results.push(response);
            completed++;
            if (completed === uploads.length) {
              observer.next(results);
              observer.complete();
            }
          },
          error: (error) => {
            observer.error(error);
          }
        });
      });
    });
  }

  // ============================================
  // Error Handling
  // ============================================

  private handleError(error: any): Observable<never> {
    console.error('DocumentService Error:', error);
    
    let errorMessage = 'Une erreur est survenue';
    
    if (error.error?.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    } else if (error.status === 0) {
      errorMessage = 'Impossible de contacter le serveur';
    } else if (error.status === 401) {
      errorMessage = 'Non autorisé';
    } else if (error.status === 403) {
      errorMessage = 'Accès refusé';
    } else if (error.status === 404) {
      errorMessage = 'Document non trouvé';
    } else if (error.status === 413) {
      errorMessage = 'Fichier trop volumineux';
    } else if (error.status === 415) {
      errorMessage = 'Format de fichier non supporté';
    } else if (error.status >= 500) {
      errorMessage = 'Erreur serveur';
    }

    return throwError(() => ({ message: errorMessage, originalError: error }));
  }
}
