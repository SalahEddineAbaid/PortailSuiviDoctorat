import { Component, Input, OnInit, OnDestroy, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Observable, BehaviorSubject, Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';

import { DocumentService } from '../../../core/services/document.service';
import { DocumentResponse, DocumentType } from '../../../core/models/document.model';

export interface DocumentViewerConfig {
  showToolbar?: boolean;
  showDownload?: boolean;
  showFullscreen?: boolean;
  height?: string;
  width?: string;
}

@Component({
  selector: 'app-document-viewer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './document-viewer.component.html',
  styleUrls: ['./document-viewer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DocumentViewerComponent implements OnInit, OnDestroy {
  @Input() document!: DocumentResponse;
  @Input() config: DocumentViewerConfig = {};
  
  private destroy$ = new Subject<void>();
  private loadingSubject = new BehaviorSubject<boolean>(false);
  private errorSubject = new BehaviorSubject<string | null>(null);
  
  loading$ = this.loadingSubject.asObservable();
  error$ = this.errorSubject.asObservable();
  
  documentUrl: SafeResourceUrl | null = null;
  isImage = false;
  isPdf = false;
  isFullscreen = false;
  
  // Default configuration
  private defaultConfig: DocumentViewerConfig = {
    showToolbar: true,
    showDownload: true,
    showFullscreen: true,
    height: '600px',
    width: '100%'
  };

  constructor(
    private documentService: DocumentService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.validateInputs();
    this.applyDefaultConfig();
    this.loadDocument();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.cleanupUrl();
  }

  private validateInputs(): void {
    if (!this.document) {
      throw new Error('DocumentViewer: document is required');
    }
  }

  private applyDefaultConfig(): void {
    this.config = { ...this.defaultConfig, ...this.config };
  }

  private loadDocument(): void {
    this.loadingSubject.next(true);
    this.errorSubject.next(null);

    this.documentService.downloadDocument(this.document.id)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loadingSubject.next(false))
      )
      .subscribe({
        next: (blob) => {
          this.handleDocumentBlob(blob);
        },
        error: (error) => {
          console.error('Erreur chargement document:', error);
          this.errorSubject.next('Impossible de charger le document');
        }
      });
  }

  private handleDocumentBlob(blob: Blob): void {
    this.cleanupUrl();
    
    const url = URL.createObjectURL(blob);
    this.documentUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
    
    // Determine document type for display
    this.isImage = blob.type.startsWith('image/');
    this.isPdf = blob.type === 'application/pdf';
  }

  private cleanupUrl(): void {
    if (this.documentUrl) {
      const url = (this.documentUrl as any).changingThisBreaksApplicationSecurity;
      if (url && url.startsWith('blob:')) {
        URL.revokeObjectURL(url);
      }
    }
  }

  onDownload(): void {
    this.documentService.downloadDocument(this.document.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = this.document.nom;
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          window.URL.revokeObjectURL(url);
        },
        error: (error) => {
          console.error('Erreur téléchargement:', error);
          this.errorSubject.next('Erreur lors du téléchargement');
        }
      });
  }

  onFullscreen(): void {
    this.isFullscreen = !this.isFullscreen;
    
    if (this.isFullscreen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
  }

  onCloseFullscreen(): void {
    this.isFullscreen = false;
    document.body.style.overflow = '';
  }

  onRetry(): void {
    this.loadDocument();
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

  get viewerHeight(): string {
    return this.config.height || '600px';
  }

  get viewerWidth(): string {
    return this.config.width || '100%';
  }

  get canPreview(): boolean {
    return this.isImage || this.isPdf;
  }

  get showToolbar(): boolean {
    return this.config.showToolbar !== false;
  }

  get showDownload(): boolean {
    return this.config.showDownload !== false;
  }

  get showFullscreen(): boolean {
    return this.config.showFullscreen !== false && this.canPreview;
  }
}