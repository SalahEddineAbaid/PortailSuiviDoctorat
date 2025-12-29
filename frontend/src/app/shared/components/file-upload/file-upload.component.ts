import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy, forwardRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export interface FileUploadConfig {
  maxSize?: number; // in MB
  allowedTypes?: string[];
  multiple?: boolean;
  accept?: string;
}

export interface UploadedFile {
  file: File;
  id: string;
  progress?: number;
  error?: string;
  uploaded?: boolean;
}

@Component({
  selector: 'app-file-upload',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FileUploadComponent),
      multi: true
    }
  ],
  template: `
    <div class="file-upload" [class.disabled]="disabled" [class.error]="hasError">
      <div 
        class="upload-zone"
        [class.dragover]="isDragOver"
        (click)="fileInput.click()"
        (dragover)="onDragOver($event)"
        (dragleave)="onDragLeave($event)"
        (drop)="onDrop($event)"
        [attr.tabindex]="disabled ? -1 : 0"
        (keydown.enter)="fileInput.click()"
        (keydown.space)="fileInput.click()"
        role="button"
        [attr.aria-label]="ariaLabel"
        [attr.aria-describedby]="errorId"
      >
        <input
          #fileInput
          type="file"
          class="file-input"
          [accept]="config.accept || '*'"
          [multiple]="config.multiple || false"
          [disabled]="disabled"
          (change)="onFileSelect($event)"
          [attr.aria-hidden]="true"
        />
        
        <div class="upload-content">
          <span class="material-icons upload-icon" aria-hidden="true">cloud_upload</span>
          <div class="upload-text">
            <div class="primary-text">{{ primaryText }}</div>
            <div class="secondary-text">{{ secondaryText }}</div>
          </div>
        </div>
      </div>
      
      <div *ngIf="files.length > 0" class="file-list">
        <div 
          *ngFor="let uploadedFile of files; trackBy: trackByFileId"
          class="file-item"
          [class.error]="uploadedFile.error"
          [class.uploaded]="uploadedFile.uploaded"
        >
          <div class="file-info">
            <span class="material-icons file-icon" aria-hidden="true">
              {{ getFileIcon(uploadedFile.file) }}
            </span>
            <div class="file-details">
              <div class="file-name">{{ uploadedFile.file.name }}</div>
              <div class="file-size">{{ formatFileSize(uploadedFile.file.size) }}</div>
            </div>
          </div>
          
          <div class="file-actions">
            <div *ngIf="uploadedFile.progress !== undefined && !uploadedFile.uploaded && !uploadedFile.error" 
                 class="progress-container">
              <div class="progress-bar">
                <div 
                  class="progress-fill" 
                  [style.width.%]="uploadedFile.progress"
                ></div>
              </div>
              <span class="progress-text">{{ uploadedFile.progress }}%</span>
            </div>
            
            <span *ngIf="uploadedFile.uploaded" class="material-icons success-icon" aria-hidden="true">
              check_circle
            </span>
            
            <span *ngIf="uploadedFile.error" class="material-icons error-icon" aria-hidden="true">
              error
            </span>
            
            <button
              type="button"
              class="remove-btn"
              (click)="removeFile(uploadedFile.id)"
              [attr.aria-label]="'Supprimer ' + uploadedFile.file.name"
              [disabled]="disabled"
            >
              <span class="material-icons" aria-hidden="true">close</span>
            </button>
          </div>
        </div>
      </div>
      
      <div *ngIf="hasError" class="error-message" [id]="errorId" role="alert">
        {{ errorMessage }}
      </div>
      
      <div *ngIf="helperText" class="helper-text">
        {{ helperText }}
      </div>
    </div>
  `,
  styleUrls: ['./file-upload.component.scss']
})
export class FileUploadComponent implements ControlValueAccessor {
  @Input() config: FileUploadConfig = {};
  @Input() primaryText = 'Cliquez pour sélectionner des fichiers';
  @Input() secondaryText = 'ou glissez-déposez vos fichiers ici';
  @Input() helperText = '';
  @Input() errorMessage = '';
  @Input() disabled = false;
  @Input() ariaLabel = 'Zone de téléchargement de fichiers';
  
  @Output() filesSelected = new EventEmitter<File[]>();
  @Output() fileRemoved = new EventEmitter<string>();
  @Output() uploadProgress = new EventEmitter<{ fileId: string; progress: number }>();

  files: UploadedFile[] = [];
  isDragOver = false;
  hasError = false;
  errorId = `file-upload-error-${Math.random().toString(36).substr(2, 9)}`;

  private onChange = (value: File[] | File | null) => {};
  private onTouched = () => {};

  // ControlValueAccessor implementation
  writeValue(value: File[] | File | null): void {
    if (value) {
      const filesArray = Array.isArray(value) ? value : [value];
      this.files = filesArray.map(file => ({
        file,
        id: this.generateFileId(),
        uploaded: true
      }));
    } else {
      this.files = [];
    }
  }

  registerOnChange(fn: (value: File[] | File | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    if (!this.disabled) {
      this.isDragOver = true;
    }
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
    
    if (this.disabled) return;
    
    const files = Array.from(event.dataTransfer?.files || []);
    this.handleFiles(files);
  }

  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files || []);
    this.handleFiles(files);
    input.value = ''; // Reset input
  }

  private handleFiles(files: File[]): void {
    this.hasError = false;
    this.errorMessage = '';
    
    const validFiles: File[] = [];
    
    for (const file of files) {
      const validation = this.validateFile(file);
      if (validation.valid) {
        validFiles.push(file);
      } else {
        this.hasError = true;
        this.errorMessage = validation.error || 'Fichier invalide';
        return;
      }
    }
    
    if (!this.config.multiple) {
      this.files = [];
    }
    
    const newUploadedFiles: UploadedFile[] = validFiles.map(file => ({
      file,
      id: this.generateFileId(),
      progress: 0
    }));
    
    this.files.push(...newUploadedFiles);
    this.filesSelected.emit(validFiles);
    this.updateFormValue();
    this.onTouched();
  }

  private validateFile(file: File): { valid: boolean; error?: string } {
    // Check file size
    if (this.config.maxSize && file.size > this.config.maxSize * 1024 * 1024) {
      return {
        valid: false,
        error: `Le fichier est trop volumineux. Taille maximale: ${this.config.maxSize}MB`
      };
    }
    
    // Check file type
    if (this.config.allowedTypes && this.config.allowedTypes.length > 0) {
      const fileType = file.type;
      const fileExtension = file.name.split('.').pop()?.toLowerCase();
      
      const isValidType = this.config.allowedTypes.some(type => {
        if (type.startsWith('.')) {
          return fileExtension === type.substring(1);
        }
        return fileType === type || fileType.startsWith(type.split('/')[0] + '/');
      });
      
      if (!isValidType) {
        return {
          valid: false,
          error: `Type de fichier non autorisé. Types acceptés: ${this.config.allowedTypes.join(', ')}`
        };
      }
    }
    
    return { valid: true };
  }

  removeFile(fileId: string): void {
    this.files = this.files.filter(f => f.id !== fileId);
    this.fileRemoved.emit(fileId);
    this.updateFormValue();
    this.onTouched();
  }

  private updateFormValue(): void {
    const files = this.files.map(f => f.file);
    const value = this.config.multiple ? files : (files[0] || null);
    this.onChange(value);
  }

  private generateFileId(): string {
    return Math.random().toString(36).substr(2, 9);
  }

  trackByFileId(index: number, file: UploadedFile): string {
    return file.id;
  }

  getFileIcon(file: File): string {
    const type = file.type;
    if (type.startsWith('image/')) return 'image';
    if (type.includes('pdf')) return 'picture_as_pdf';
    if (type.includes('word') || type.includes('document')) return 'description';
    if (type.includes('excel') || type.includes('spreadsheet')) return 'table_chart';
    if (type.includes('powerpoint') || type.includes('presentation')) return 'slideshow';
    return 'insert_drive_file';
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  // Method to update upload progress (to be called from parent component)
  updateProgress(fileId: string, progress: number): void {
    const file = this.files.find(f => f.id === fileId);
    if (file) {
      file.progress = progress;
      if (progress === 100) {
        file.uploaded = true;
      }
      this.uploadProgress.emit({ fileId, progress });
    }
  }

  // Method to set file error (to be called from parent component)
  setFileError(fileId: string, error: string): void {
    const file = this.files.find(f => f.id === fileId);
    if (file) {
      file.error = error;
    }
  }
}