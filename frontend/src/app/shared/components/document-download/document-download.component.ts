import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { BehaviorSubject, Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';

import { DocumentService } from '../../../core/services/document.service';
import { DocumentResponse, DocumentType } from '../../../core/models/document.model';

export interface DownloadProgress {
  documentId: number;
  progress: number;
  status: 'idle' | 'downloading' | 'completed' | 'error';
}

@Component({
  selector: 'app-document-download',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatProgressSpinnerModule, MatTooltipModule, MatButtonModule],
  templateUrl: './document-download.component.html',
  styleUrls: ['./document-download.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DocumentDownloadComponent {
  @Input() document!: DocumentResponse;
  @Input() showIcon = true;
  @Input() showLabel = true;
  @Input() showSize = true;
  @Input() buttonStyle: 'button' | 'icon' | 'link' = 'button';
  @Input() disabled = false;
  
  @Output() downloadStarted = new EventEmitter<DocumentResponse>();
  @Output() downloadCompleted = new EventEmitter<DocumentResponse>();
  @Output() downloadError = new EventEmitter<{ document: DocumentResponse; error: string }>();

  private destroy$ = new Subject<void>();
  private downloadingSubject = new BehaviorSubject<boolean>(false);
  private errorSubject = new BehaviorSubject<string | null>(null);
  
  downloading$ = this.downloadingSubject.asObservable();
  error$ = this.errorSubject.asObservable();

  constructor(private documentService: DocumentService) {}

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onDownload(): void {
    if (this.disabled || this.downloadingSubject.value) {
      return;
    }

    this.downloadingSubject.next(true);
    this.errorSubject.next(null);
    this.downloadStarted.emit(this.document);

    this.documentService.downloadDocument(this.document.id)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.downloadingSubject.next(false))
      )
      .subscribe({
        next: (blob) => {
          this.handleDownload(blob);
          this.downloadCompleted.emit(this.document);
        },
        error: (error) => {
          const errorMessage = this.getErrorMessage(error);
          this.errorSubject.next(errorMessage);
          this.downloadError.emit({ document: this.document, error: errorMessage });
          console.error('Erreur téléchargement document:', error);
        }
      });
  }

  private handleDownload(blob: Blob): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    
    link.href = url;
    link.download = this.document.nom;
    link.style.display = 'none';
    
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    // Cleanup
    setTimeout(() => {
      window.URL.revokeObjectURL(url);
    }, 100);
  }

  private getErrorMessage(error: any): string {
    if (error.error?.message) {
      return error.error.message;
    }
    
    switch (error.status) {
      case 404:
        return 'Document non trouvé';
      case 403:
        return 'Accès non autorisé';
      case 500:
        return 'Erreur serveur';
      default:
        return 'Erreur lors du téléchargement';
    }
  }

  // Helper methods for template
  get documentIcon(): string {
    return this.documentService.getDocumentTypeIcon(this.document.type);
  }

  get documentTypeLabel(): string {
    return this.documentService.getDocumentTypeLabel(this.document.type);
  }

  get formattedFileSize(): string {
    return this.documentService.formatFileSize(this.document.taille);
  }

  get formattedDate(): string {
    return new Date(this.document.dateUpload).toLocaleDateString('fr-FR');
  }

  get downloadLabel(): string {
    return this.showLabel ? this.document.nom : 'Télécharger';
  }

  get isButtonStyle(): boolean {
    return this.buttonStyle === 'button';
  }

  get isIconStyle(): boolean {
    return this.buttonStyle === 'icon';
  }

  get isLinkStyle(): boolean {
    return this.buttonStyle === 'link';
  }

  get canDownload(): boolean {
    return !this.disabled && !this.downloadingSubject.value;
  }
}