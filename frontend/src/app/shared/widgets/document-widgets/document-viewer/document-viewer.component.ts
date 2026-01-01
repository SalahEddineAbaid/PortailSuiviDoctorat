import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

export interface Document {
    id: number;
    name: string;
    type: string;  // 'pdf', 'jpg', 'png', etc.
    url: string;
    size?: number;
    uploadDate?: Date;
}

/**
 * Document Viewer Component
 * View documents (PDF, images) inline
 * 
 * @example
 * <app-document-viewer
 *   [document]="currentDocument"
 *   [showControls]="true"
 *   (download)="onDownload($event)">
 * </app-document-viewer>
 */
@Component({
    selector: 'app-document-viewer',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="document-viewer" *ngIf="document">
      <!-- Viewer Container -->
      <div class="viewer-container">
        <!-- PDF Viewer -->
        <iframe 
          *ngIf="isPdf(document)"
          [src]="getSafeUrl(document.url)"
          class="pdf-viewer"
          [title]="document.name">
        </iframe>
        
        <!-- Image Viewer -->
        <div *ngIf="isImage(document)" class="image-viewer">
          <img 
            [src]="document.url"
            [alt]="document.name"
            [style.transform]="'scale(' + zoom / 100 + ')'">
        </div>
        
        <!-- Unsupported Type -->
        <div *ngIf="!isPdf(document) && !isImage(document)" class="unsupported-viewer">
          <i class="icon-file"></i>
          <h3>Prévisualisation non disponible</h3>
          <p>Type de fichier: {{ document.type.toUpperCase() }}</p>
          <button (click)="onDownloadClick()" class="btn btn-primary">
            <i class="icon-download"></i>
            Télécharger le fichier
          </button>
        </div>
      </div>

      <!-- Controls -->
      <div class="viewer-controls" *ngIf="showControls">
        <div class="controls-group">
          <span class="document-name">{{ document.name }}</span>
          <span class="document-size" *ngIf="document.size">
            ({{ formatFileSize(document.size) }})
          </span>
        </div>
        
        <div class="controls-group">
          <!-- Zoom Controls (Image only) -->
          <div *ngIf="isImage(document)" class="zoom-controls">
            <button 
              (click)="zoomOut()"
              [disabled]="zoom <= 50"
              class="btn-icon"
              type="button"
              title="Zoom out">
              <i class="icon-zoom-out"></i>
            </button>
            <span class="zoom-level">{{ zoom }}%</span>
            <button 
              (click)="zoomIn()"
              [disabled]="zoom >= 200"
              class="btn-icon"
              type="button"
              title="Zoom in">
              <i class="icon-zoom-in"></i>
            </button>
            <button 
              (click)="resetZoom()"
              class="btn-icon"
              type="button"
              title="Reset zoom">
              <i class="icon-maximize"></i>
            </button>
          </div>
          
          <!-- Download Button -->
          <button 
            (click)="onDownloadClick()"
            class="btn btn-secondary"
            type="button">
            <i class="icon-download"></i>
            Télécharger
          </button>
          
          <!-- Close Button -->
          <button 
            *ngIf="showClose"
            (click)="onCloseClick()"
            class="btn btn-secondary"
            type="button">
            <i class="icon-x"></i>
            Fermer
          </button>
        </div>
      </div>
    </div>
  `,
    styles: [`
    .document-viewer {
      display: flex;
      flex-direction: column;
      height: 100%;
      background-color: #f3f4f6;
      border-radius: 0.5rem;
      overflow: hidden;
    }

    .viewer-container {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      overflow: auto;
      background-color: #1f2937;
      position: relative;
    }

    .pdf-viewer {
      width: 100%;
      height: 100%;
      border: none;
    }

    .image-viewer {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 2rem;
      overflow: auto;
    }

    .image-viewer img {
      max-width: 100%;
      max-height: 100%;
      object-fit: contain;
      transition: transform 0.2s;
      cursor: zoom-in;
    }

    .unsupported-viewer {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 1rem;
      padding: 3rem;
      color: white;
      text-align: center;
    }

    .unsupported-viewer i {
      font-size: 4rem;
      opacity: 0.5;
    }

    .unsupported-viewer h3 {
      margin: 0;
      font-size: 1.5rem;
    }

    .unsupported-viewer p {
      margin: 0;
      opacity: 0.7;
    }

    .viewer-controls {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1rem 1.5rem;
      background-color: white;
      border-top: 1px solid #e5e7eb;
      gap: 1rem;
      flex-wrap: wrap;
    }

    .controls-group {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .document-name {
      font-weight: 500;
      color: #111827;
    }

    .document-size {
      color: #6b7280;
      font-size: 0.875rem;
    }

    .zoom-controls {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.25rem;
      background-color: #f9fafb;
      border-radius: 0.375rem;
    }

    .zoom-level {
      min-width: 3rem;
      text-align: center;
      font-weight: 500;
      font-size: 0.875rem;
      color: #6b7280;
    }

    .btn {
      padding: 0.5rem 1rem;
      border-radius: 0.375rem;
      font-weight: 500;
      cursor: pointer;
      border: none;
      transition: all 0.2s;
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.875rem;
    }

    .btn-primary {
      background-color: #3b82f6;
      color: white;
    }

    .btn-primary:hover {
      background-color: #2563eb;
    }

    .btn-secondary {
      background-color: #6b7280;
      color: white;
    }

    .btn-secondary:hover {
      background-color: #4b5563;
    }

    .btn-icon {
      background: none;
      border: none;
      cursor: pointer;
      padding: 0.375rem;
      border-radius: 0.25rem;
      color: #6b7280;
      transition: all 0.2s;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .btn-icon:hover:not(:disabled) {
      background-color: #e5e7eb;
      color: #111827;
    }

    .btn-icon:disabled {
      opacity: 0.3;
      cursor: not-allowed;
    }

    @media (max-width: 640px) {
      .viewer-controls {
        flex-direction: column;
        align-items: stretch;
      }

      .controls-group {
        justify-content: center;
      }
    }
  `]
})
export class DocumentViewerComponent {
    @Input() document!: Document;
    @Input() showControls: boolean = true;
    @Input() showClose: boolean = false;

    @Output() download = new EventEmitter<Document>();
    @Output() close = new EventEmitter<void>();

    zoom = 100;

    constructor(private sanitizer: DomSanitizer) { }

    isPdf(doc: Document): boolean {
        return doc.type.toLowerCase() === 'pdf';
    }

    isImage(doc: Document): boolean {
        const imageTypes = ['jpg', 'jpeg', 'png', 'gif', 'webp'];
        return imageTypes.includes(doc.type.toLowerCase());
    }

    getSafeUrl(url: string): SafeResourceUrl {
        return this.sanitizer.bypassSecurityTrustResourceUrl(url);
    }

    zoomIn(): void {
        this.zoom = Math.min(200, this.zoom + 25);
    }

    zoomOut(): void {
        this.zoom = Math.max(50, this.zoom - 25);
    }

    resetZoom(): void {
        this.zoom = 100;
    }

    onDownloadClick(): void {
        this.download.emit(this.document);
    }

    onCloseClick(): void {
        this.close.emit();
    }

    formatFileSize(bytes: number): string {
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
    }
}
