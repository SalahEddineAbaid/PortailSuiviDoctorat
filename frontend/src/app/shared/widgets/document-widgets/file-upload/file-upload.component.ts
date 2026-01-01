import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpEventType } from '@angular/common/http';

export interface UploadConfig {
    maxSize: number;          // Mo
    allowedTypes: string[];   // ['pdf', 'jpg', 'png']
    multiple: boolean;
    required: boolean;
    autoUpload?: boolean;
}

export interface FileUploadResult {
    file: File;
    url?: string;
    documentId?: number;
    status: 'pending' | 'uploading' | 'success' | 'error';
    progress?: number;
    error?: string;
}

/**
 * File Upload Component
 * Drag & drop file upload with progress tracking
 * 
 * @example
 * <app-file-upload 
 *   [config]="uploadConfig"
 *   (filesSelected)="onFilesSelected($event)"
 *   (uploadComplete)="onUploadComplete($event)">
 * </app-file-upload>
 */
@Component({
    selector: 'app-file-upload',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="file-upload-container">
      <!-- Drop Zone -->
      <div 
        class="drop-zone"
        [class.dragging]="isDragging"
        [class.disabled]="disabled"
        (drop)="onDrop($event)"
        (dragover)="onDragOver($event)"
        (dragenter)="onDragEnter($event)"
        (dragleave)="onDragLeave($event)">
        
        <input 
          #fileInput
          type="file"
          [multiple]="config.multiple"
          [accept]="getAcceptedTypes()"
          (change)="onFileSelect($event)"
          [disabled]="disabled"
          class="file-input">
        
        <div class="drop-zone-content">
          <i class="icon-upload"></i>
          <p class="drop-zone-text">
            <strong>{{ label }}</strong>
            <span>ou cliquez pour sélectionner</span>
          </p>
          <button 
            type="button"
            (click)="fileInput.click()"
            [disabled]="disabled"
            class="btn btn-primary">
            Choisir fichier(s)
          </button>
          <small class="drop-zone-hint">
            Types acceptés: {{ config.allowedTypes.join(', ').toUpperCase() }} 
            | Taille max: {{ config.maxSize }}Mo
          </small>
        </div>
      </div>

      <!-- Files List -->
      <div class="files-list" *ngIf="files.length > 0">
        <h4>Fichiers ({{ files.length }})</h4>
        
        <div 
          *ngFor="let fileResult of files; let i = index"
          class="file-item"
          [class]="'status-' + fileResult.status">
          
          <div class="file-info">
            <i class="file-icon icon-{{getFileIcon(fileResult.file)}}"></i>
            <div class="file-details">
              <div class="file-name">{{ fileResult.file.name }}</div>
              <div class="file-size">{{ formatFileSize(fileResult.file.size) }}</div>
            </div>
          </div>
          
          <!-- Progress Bar -->
          <div *ngIf="fileResult.status === 'uploading'" class="file-progress">
            <div class="progress-bar">
              <div 
                class="progress-fill"
                [style.width.%]="fileResult.progress">
              </div>
            </div>
            <span class="progress-text">{{ fileResult.progress }}%</span>
          </div>
          
          <!-- Status Icons -->
          <div class="file-status">
            <i *ngIf="fileResult.status === 'pending'" class="icon-clock text-gray"></i>
            <i *ngIf="fileResult.status === 'uploading'" class="icon-loader text-blue"></i>
            <i *ngIf="fileResult.status === 'success'" class="icon-check-circle text-green"></i>
            <i *ngIf="fileResult.status === 'error'" class="icon-x-circle text-red"></i>
          </div>
          
          <!-- Actions -->
          <div class="file-actions">
            <button 
              *ngIf="fileResult.status === 'pending'"
              (click)="uploadFile(fileResult)"
              class="btn-icon"
              type="button"
              title="Upload">
              <i class="icon-upload"></i>
            </button>
            <button 
              (click)="remove(i)"
              class="btn-icon btn-danger"
              type="button"
              title="Supprimer">
              <i class="icon-trash"></i>
            </button>
          </div>
          
          <!-- Error Message -->
          <div *ngIf="fileResult.error" class="file-error">
            {{ fileResult.error }}
          </div>
        </div>
        
        <!-- Bulk Actions -->
        <div class="bulk-actions" *ngIf="hasPendingFiles()">
          <button 
            (click)="uploadAll()"
            class="btn btn-primary"
            type="button">
            <i class="icon-upload"></i>
            Upload tout
          </button>
          <button 
            (click)="clearAll()"
            class="btn btn-secondary"
            type="button">
            <i class="icon-x"></i>
            Tout supprimer
          </button>
        </div>
      </div>
    </div>
  `,
    styles: [`
    .file-upload-container {
      width: 100%;
    }

    .drop-zone {
      border: 2px dashed #d1d5db;
      border-radius: 0.5rem;
      padding: 2rem;
      text-align: center;
      transition: all 0.2s;
      background-color: #f9fafb;
      cursor: pointer;
    }

    .drop-zone:hover:not(.disabled) {
      border-color: #3b82f6;
      background-color: #eff6ff;
    }

    .drop-zone.dragging {
      border-color: #3b82f6;
      background-color: #dbeafe;
      transform: scale(1.02);
    }

    .drop-zone.disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .file-input {
      display: none;
    }

    .drop-zone-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
    }

    .drop-zone-content i {
      font-size: 3rem;
      color: #9ca3af;
    }

    .drop-zone-text {
      margin: 0;
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .drop-zone-text strong {
      color: #111827;
      font-size: 1rem;
    }

    .drop-zone-text span {
      color: #6b7280;
      font-size: 0.875rem;
    }

    .drop-zone-hint {
      color: #9ca3af;
      font-size: 0.75rem;
    }

    .btn {
      padding: 0.625rem 1.25rem;
      border-radius: 0.375rem;
      font-weight: 500;
      cursor: pointer;
      border: none;
      transition: all 0.2s;
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
    }

    .btn-primary {
      background-color: #3b82f6;
      color: white;
    }

    .btn-primary:hover:not(:disabled) {
      background-color: #2563eb;
    }

    .btn-secondary {
      background-color: #6b7280;
      color: white;
    }

    .btn-secondary:hover:not(:disabled) {
      background-color: #4b5563;
    }

    .btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .files-list {
      margin-top: 1.5rem;
    }

    .files-list h4 {
      margin: 0 0 1rem 0;
      font-size: 1rem;
      font-weight: 600;
      color: #111827;
    }

    .file-item {
      display: grid;
      grid-template-columns: 1fr auto auto;
      gap: 1rem;
      align-items: center;
      padding: 1rem;
      background-color: white;
      border: 1px solid #e5e7eb;
      border-radius: 0.375rem;
      margin-bottom: 0.75rem;
    }

    .file-info {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      min-width: 0;
    }

    .file-icon {
      font-size: 1.5rem;
      color: #6b7280;
      flex-shrink: 0;
    }

    .file-details {
      min-width: 0;
      flex: 1;
    }

    .file-name {
      font-weight: 500;
      color: #111827;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .file-size {
      font-size: 0.75rem;
      color: #9ca3af;
    }

    .file-progress {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      min-width: 150px;
    }

    .progress-bar {
      flex: 1;
      height: 6px;
      background-color: #e5e7eb;
      border-radius: 9999px;
      overflow: hidden;
    }

    .progress-fill {
      height: 100%;
      background-color: #3b82f6;
      transition: width 0.3s;
    }

    .progress-text {
      font-size: 0.75rem;
      font-weight: 600;
      color: #6b7280;
      min-width: 3rem;
      text-align: right;
    }

    .file-status i {
      font-size: 1.25rem;
    }

    .text-gray { color: #9ca3af; }
    .text-blue { color: #3b82f6; animation: spin 1s linear infinite; }
    .text-green { color: #10b981; }
    .text-red { color: #ef4444; }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .file-actions {
      display: flex;
      gap: 0.5rem;
    }

    .btn-icon {
      background: none;
      border: none;
      cursor: pointer;
      padding: 0.375rem;
      border-radius: 0.25rem;
      color: #6b7280;
      transition: all 0.2s;
    }

    .btn-icon:hover {
      background-color: #f3f4f6;
      color: #111827;
    }

    .btn-icon.btn-danger:hover {
      background-color: #fee2e2;
      color: #dc2626;
    }

    .file-error {
      grid-column: 1 / -1;
      padding: 0.5rem;
      background-color: #fee2e2;
      color: #991b1b;
      font-size: 0.875rem;
      border-radius: 0.25rem;
    }

    .bulk-actions {
      display: flex;
      gap: 0.75rem;
      margin-top: 1rem;
      padding-top: 1rem;
      border-top: 1px solid #e5e7eb;
    }

    @media (max-width: 640px) {
      .file-item {
        grid-template-columns: 1fr;
      }

      .file-progress {
        grid-column: 1;
      }

      .file-actions {
        justify-content: flex-end;
      }
    }
  `]
})
export class FileUploadComponent {
    @Input() config!: UploadConfig;
    @Input() label: string = 'Déposer fichiers ici';
    @Input() disabled: boolean = false;

    @Output() filesSelected = new EventEmitter<File[]>();
    @Output() uploadComplete = new EventEmitter<FileUploadResult[]>();
    @Output() uploadError = new EventEmitter<{ file: File, error: Error }>();

    files: FileUploadResult[] = [];
    isDragging = false;

    onDrop(event: DragEvent): void {
        event.preventDefault();
        this.isDragging = false;

        if (this.disabled) return;

        const files = Array.from(event.dataTransfer?.files || []);
        this.handleFiles(files);
    }

    onDragOver(event: DragEvent): void {
        event.preventDefault();
    }

    onDragEnter(event: DragEvent): void {
        event.preventDefault();
        this.isDragging = true;
    }

    onDragLeave(event: DragEvent): void {
        event.preventDefault();
        this.isDragging = false;
    }

    onFileSelect(event: Event): void {
        const input = event.target as HTMLInputElement;
        const files = Array.from(input.files || []);
        this.handleFiles(files);
        input.value = ''; // Reset input
    }

    handleFiles(files: File[]): void {
        const validFiles: File[] = [];

        files.forEach(file => {
            const validation = this.validateFile(file);
            if (validation.valid) {
                validFiles.push(file);
                this.files.push({
                    file,
                    status: 'pending',
                    progress: 0
                });
            } else {
                this.files.push({
                    file,
                    status: 'error',
                    error: validation.error
                });
            }
        });

        if (validFiles.length > 0) {
            this.filesSelected.emit(validFiles);

            if (this.config.autoUpload) {
                this.uploadAll();
            }
        }
    }

    validateFile(file: File): { valid: boolean, error?: string } {
        // Check size
        const maxBytes = this.config.maxSize * 1024 * 1024;
        if (file.size > maxBytes) {
            return {
                valid: false,
                error: `Fichier trop volumineux (max ${this.config.maxSize}Mo)`
            };
        }

        // Check type
        const extension = file.name.split('.').pop()?.toLowerCase();
        if (!extension || !this.config.allowedTypes.includes(extension)) {
            return {
                valid: false,
                error: `Type de fichier non autorisé (${extension})`
            };
        }

        return { valid: true };
    }

    uploadFile(fileResult: FileUploadResult): void {
        fileResult.status = 'uploading';
        fileResult.progress = 0;

        // Simulate upload (replace with actual DocumentService call)
        const interval = setInterval(() => {
            if (fileResult.progress! < 100) {
                fileResult.progress = Math.min(100, fileResult.progress! + 10);
            } else {
                clearInterval(interval);
                fileResult.status = 'success';
                fileResult.documentId = Math.floor(Math.random() * 10000);
                this.uploadComplete.emit([fileResult]);
            }
        }, 200);
    }

    uploadAll(): void {
        this.files
            .filter(f => f.status === 'pending')
            .forEach(f => this.uploadFile(f));
    }

    remove(index: number): void {
        this.files.splice(index, 1);
    }

    clearAll(): void {
        this.files = this.files.filter(f => f.status === 'success');
    }

    hasPendingFiles(): boolean {
        return this.files.some(f => f.status === 'pending');
    }

    getAcceptedTypes(): string {
        return this.config.allowedTypes.map(t => `.${t}`).join(',');
    }

    getFileIcon(file: File): string {
        const ext = file.name.split('.').pop()?.toLowerCase();
        const iconMap: Record<string, string> = {
            'pdf': 'file-text',
            'doc': 'file-text',
            'docx': 'file-text',
            'jpg': 'image',
            'jpeg': 'image',
            'png': 'image',
            'gif': 'image'
        };
        return iconMap[ext || ''] || 'file';
    }

    formatFileSize(bytes: number): string {
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
    }
}
